package com.canehealth.spring.ctakes.service;


/**
 * @author sanar
 *
 */

import akka.actor.ActorSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.json.JsonCasSerializer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.canehealth.spring.ctakes.MyClinicalPipelineFactory;
import com.canehealth.spring.ctakes.MyPipeline;


import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Collection;


@Service
@CacheConfig(cacheNames = "ctakes")
public class CtakesService {

	private static Log log = LogFactory.getLog(CtakesService.class);

	public final JCas jcas;
	public final AnalysisEngineDescription aed;

	
//	private String pipeline = "FAST";
	@Value("${ctakes.pipeline}")
	private String pipeline = "FAST";
	@Autowired
	private ActorSystem system;

	public CtakesService() throws Exception {
		jcas = JCasFactory.createJCas();
		aed = MyPipeline.getAggregateBuilder();
	}

	@CacheEvict(allEntries = true)
	public void clearCache() {
	}

	@Cacheable(value = "ctakes")
	public String Jcas2json(String note) throws Exception {
	
		jcas.reset();
		jcas.setDocumentText(note);
		SimplePipeline.runPipeline(jcas, aed);
		CAS cas = jcas.getCas();
		JsonCasSerializer jcs = new JsonCasSerializer();
		jcs.setPrettyPrint(true);
		
		
        jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitSubtypes);
        jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitContext);
        jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitExpandedTypeNames);
		StringWriter sw = new StringWriter();
		jcs.serialize(cas, sw); 
		return jsonClinical(sw.toString(), note);
	}

	@SuppressWarnings("unchecked")
	public String jsonClinical(String ctakes, String note) throws Exception {
		JSONParser jsonParser = new JSONParser();
		JSONObject obj = new JSONObject();
		try {
			obj = (JSONObject) jsonParser.parse(ctakes);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		obj = (JSONObject) obj.get("_views");
		obj = (JSONObject) obj.get("_InitialView");
		
		
		
		//alihur edits - obj = (JSONObject) obj.get("_InitialView");
		JSONArray StrengthAnnotation = (JSONArray) obj.get("StrengthAnnotation");
		JSONArray FormAnnotation = (JSONArray) obj.get("FormAnnotation");
		JSONArray MeasurementAnnotation = (JSONArray) obj.get("MeasurementAnnotation");
		JSONArray DrugChangeStatusAnnotation = (JSONArray) obj.get("DrugChangeStatusAnnotation");
		JSONArray MedicationMention = (JSONArray) obj.get("MedicationMention");
		JSONArray AnatomicalSiteMention = (JSONArray) obj.get("AnatomicalSiteMention");
		JSONArray DiseaseDisorderMention = (JSONArray) obj.get("DiseaseDisorderMention");
		JSONArray SignSymptomMention = (JSONArray) obj.get("SignSymptomMention");
		JSONArray ProcedureMention = (JSONArray) obj.get("ProcedureMention");
		JSONArray WordToken = (JSONArray) obj.get("WordToken");

		JSONObject output = new JSONObject();
	
	//Sana's Edits 
		// this is only needed to show you original text in the following annotations. You can add other annotation types too. 		
		// Note: all these changes are made in 'initial_view' obj in order to show others annotations too
		 
		if(StrengthAnnotation!=null)
			obj.put("StrengthAnnotation", parseJsonMention(note, WordToken, StrengthAnnotation));
		if(FormAnnotation!=null)
		obj.put("FormAnnotation", parseJsonMention(note, WordToken, FormAnnotation));
		if(MedicationMention!=null)
			obj.put("MedicationMention", parseJsonMention(note, WordToken, MedicationMention));
		if(DrugChangeStatusAnnotation!=null)
			obj.put("DrugChangeStatusAnnotation", parseJsonMention(note, WordToken, DrugChangeStatusAnnotation));
		if(AnatomicalSiteMention!=null)
		obj.put("AnatomicalSiteMention", parseJsonMention(note, WordToken, AnatomicalSiteMention));
		if(DiseaseDisorderMention!=null)
		obj.put("DiseaseDisorderMention", parseJsonMention(note, WordToken, DiseaseDisorderMention));
		if(SignSymptomMention!=null)
		obj.put("SignSymptomMention", parseJsonMention(note, WordToken, SignSymptomMention));
		if(ProcedureMention!=null)
		obj.put("ProcedureMention", parseJsonMention(note, WordToken, ProcedureMention));
		if(MeasurementAnnotation!=null)
		  obj.put("MeasurementAnnotation", parseJsonMention(note, WordToken, MeasurementAnnotation));
			
		
		output.put("Original", obj);
			
		return output.toJSONString();
		
		
		// for relation references, CAS_Top object is needed. uncomment this line in that case 
		
	//	return ctakes;      
	}

	private JSONArray parseJsonMention(String document, JSONArray wordtoken, JSONArray jsonArray) throws Exception {

		JSONArray output = new JSONArray();
	
			for (int i = 0, size = jsonArray.size(); i < size; i++) {
				// this check is needed if you want to add "original_text" attribue for other annotators too
				if(!(jsonArray.get(i) instanceof JSONObject))
				  continue;
				JSONObject objectInArray = (JSONObject) jsonArray.get(i);
		       
				long begin = (long) objectInArray.get("begin");
				long end = (long) objectInArray.get("end");
				String original_word = document.substring((int) begin, (int) end);
				String canonical_form = "";
				for (int i2 = 0, size2 = wordtoken.size(); i2 < size2; i2++) {
					JSONObject tokenInArray = (JSONObject) wordtoken.get(i2);
					long begin2 = (long) tokenInArray.get("begin");
					long end2 = (long) tokenInArray.get("end");
					if (begin == begin2 && end == end2)
						canonical_form = (String) tokenInArray.get("canonicalForm");
				}
				objectInArray.put("originalWord", original_word);
				objectInArray.put("canonicalForm", canonical_form);
				output.add(objectInArray);
			}
			return output;
	
	}
	
	
	private String dummyFunction(String jsonString, String note) {
		
		
		
		return jsonString;
		
		
		
	}

}
