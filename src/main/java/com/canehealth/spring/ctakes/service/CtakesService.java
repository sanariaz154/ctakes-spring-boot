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
		/*switch (pipeline) {
		case "FAST":
			aed = MyClinicalPipelineFactory.getFastPipeline();
			break;
		case "DEFAULT":
			aed = MyClinicalPipelineFactory.getCda2Pipeline();
			break;
		case "NP":
			aed = MyClinicalPipelineFactory.getNpChunkerPipeline();
			break;
		case "COREF":
			aed = MyClinicalPipelineFactory.getCoreferencePipeline();
			break;
		case "PARSE":
			aed = MyClinicalPipelineFactory.getParsingPipeline();
			break;
		case "CHUNK":
			aed = MyClinicalPipelineFactory.getStandardChunkAdjusterAnnotator();
			break;
		case "TOKEN":
			aed = MyClinicalPipelineFactory.getTokenProcessingPipeline();
			break;
		default:
			aed = MyClinicalPipelineFactory.getDefaultPipeline();
		}*/
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
		
		
		// do some configuration
        jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitSubtypes);
        jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitContext);
        jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitExpandedTypeNames);

		StringWriter sw = new StringWriter();
		jcs.serialize(cas, sw); // serialize into sw
		//return jsonClinical(sw.toString(), note);
		 return sw.toString();
	}

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
		
		//sana's edit
		//JSONArray TimeMention = (JSONArray) obj.get("TimeMention"); // time is in long.....
		JSONArray MeasurementAnnotation = (JSONArray) obj.get("MeasurementAnnotation");
		
		// ----
		
		JSONArray MedicationMention = (JSONArray) obj.get("MedicationMention");
		JSONArray AnatomicalSiteMention = (JSONArray) obj.get("AnatomicalSiteMention");
		JSONArray DiseaseDisorderMention = (JSONArray) obj.get("DiseaseDisorderMention");
		JSONArray SignSymptomMention = (JSONArray) obj.get("SignSymptomMention");
		JSONArray ProcedureMention = (JSONArray) obj.get("ProcedureMention");
		JSONArray WordToken = (JSONArray) obj.get("WordToken");

		JSONObject output = new JSONObject();
		//////////////////////
		
		
		//obj = (JSONObject) obj.get("_views");
		//obj = (JSONObject) obj.get("plaintext");
		//obj = (JSONObject) obj.get("_InitialView");
		//JSONArray MedicationMention = (JSONArray) obj.get("MedicationMention");
		//JSONArray AnatomicalSiteMention = (JSONArray) obj.get("AnatomicalSiteMention");
		//JSONArray DiseaseDisorderMention = (JSONArray) obj.get("DiseaseDisorderMention");
		//JSONArray SignSymptomMention = (JSONArray) obj.get("SignSymptomMention");
		//JSONArray ProcedureMention = (JSONArray) obj.get("ProcedureMention");
		//JSONArray WordToken = (JSONArray) obj.get("WordToken");
	
		
	/*	JSONArray EventMention = (JSONArray) obj.get("EventMention");
		JSONArray Predicate = (JSONArray) obj.get("Predicate");
		JSONArray SemanticArgument = (JSONArray) obj.get("SemanticArgument");
		JSONArray SemanticRoleRelation = (JSONArray) obj.get("SemanticRoleRelation");
		JSONArray RelationArgument = (JSONArray) obj.get("RelationArgument");
		JSONArray TemporalTextRelation = (JSONArray) obj.get("TemporalTextRelation");
		JSONArray EventProperties = (JSONArray) obj.get("EventProperties");
		//JSONArray MeasurementAnnotation = (JSONArray) obj.get("MeasurementAnnotation");
		JSONArray Event = (JSONArray) obj.get("Event");
		//JSONArray TimeMention = (JSONArray) obj.get("TimeMention");
		JSONArray RomanNumeralAnnotation = (JSONArray) obj.get("RomanNumeralAnnotation");

		JSONObject output = new JSONObject();

       
		if(MedicationMention!=null)
		output.put("MedicationMention", parseJsonMention(note, WordToken, MedicationMention));
		if(AnatomicalSiteMention!=null)
		output.put("AnatomicalSiteMention", parseJsonMention(note, WordToken, AnatomicalSiteMention));
		if(DiseaseDisorderMention!=null)
		output.put("DiseaseDisorderMention", parseJsonMention(note, WordToken, DiseaseDisorderMention));
		if(SignSymptomMention!=null)
		output.put("SignSymptomMention", parseJsonMention(note, WordToken, SignSymptomMention));
		if(ProcedureMention!=null)
		output.put("ProcedureMention", parseJsonMention(note, WordToken, ProcedureMention));

    
		if(EventMention!=null)
		output.put("EventMention", parseJsonMention(note, WordToken, EventMention));
		if(Predicate!=null)
		output.put("Predicate", parseJsonMention(note, WordToken, Predicate));
		if(SemanticArgument!=null)
		output.put("SemanticArgument", parseJsonMention(note, WordToken, SemanticArgument));
		if(SemanticRoleRelation!=null)
		output.put("SemanticRoleRelation", parseJsonMention(note, WordToken, SemanticRoleRelation));
		if(RelationArgument!=null)
		output.put("RelationArgument", parseJsonMention(note, WordToken, RelationArgument));
		if(TemporalTextRelation!=null)
		output.put("TemporalTextRelation", parseJsonMention(note, WordToken, TemporalTextRelation));
		if(EventProperties!=null)
		output.put("EventProperties", parseJsonMention(note, WordToken, EventProperties));
		if(MeasurementAnnotation!=null)
		output.put("MeasurementAnnotation", parseJsonMention(note, WordToken, MeasurementAnnotation));
		if(Event!=null)
		output.put("Event", parseJsonMention(note, WordToken, Event));
		if(TimeMention!=null)
		output.put("TimeMention", parseJsonMention(note, WordToken, TimeMention));
		if(RomanNumeralAnnotation!=null)
		output.put("RomanNumeralAnnotation", parseJsonMention(note, WordToken, RomanNumeralAnnotation));

		
		///////////////////////////
		
	*/	
		
		output.put("Original", obj);
		
		
		return output.toJSONString();
	}

	private JSONArray parseJsonMention(String document, JSONArray wordtoken, JSONArray jsonArray) throws Exception {

		JSONArray output = new JSONArray();
	
			for (int i = 0, size = jsonArray.size(); i < size; i++) {
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

}
