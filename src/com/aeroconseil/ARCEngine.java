/**
 * Project:       NDBX Business Rules Review
   Owner:         EUROCONTROL, Rue de la Fusee, 96, B-1130 Brussels, Belgium
   Provider:      AEROCONSEIL
   Title:         ARC Engine
   Summary:       Engine of the ARC Web tool for execution of Business rules

   Copyright: Developed by AEROCONSEIL for EUROCONTROL
   Contacts:
   E.POROSNICU for EUROCONTROL
   H.LEPORI for AEROCONSEIL
   P.KARP for EURILOGIC

   This work is subject to the license provided in the file LICENSE.txt.
 */

package com.aeroconseil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;


public class ARCEngine {

	public static final String EXECUTION_PATH = (ARCEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath().contains(".jar")) ? ARCEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("ARCEngine.jar", "") : "./";
	public static final String PROFIL_CONSTRUCTOR_FILE = EXECUTION_PATH + "profileConstructor.xslt";
	public static final String COMPUTED_PROFIL_FILE = "computedProfil.xsl";
	public static final String COMPUTED_RULE_FILE = "computedRule.xsl";
	public static final String GENERATE_SCHEMA_FILE = EXECUTION_PATH + "generateSchema.xsl";
	public static final String SCHEMATRON_RULE_FILE = "schema.sch";
	public static final String SCHEMATRON_ENGINE_FILE = EXECUTION_PATH + "iso_svrl.xsl";
	public static final String TEMP_DIRECTORY = EXECUTION_PATH + "tmp/";
	public static final String SCHEMATRON_VALIDATOR_FILE = "validator.xsl";
	public static final String SVRL_FILE = "SVRL_data.xml";
	public static final String GET_ERROR_ENGINE = EXECUTION_PATH + "getErrorEngine.xsl";
	public static final String GET_ERROR_FILE = "getError.xsl";
	public static final String RETREIVE_ONLY_ERROR_FILE = "retrieveOnlyError.xml";
	public static final String ERROR_TO_TABLE = EXECUTION_PATH + "errorToTable.xsl";
	public static final String ERROR_TABLE_FILE = "errorTable.xml";
	public static final String SVRL_TO_HTML = EXECUTION_PATH + "SvrlFormat.xslt";
	public static final String ENCODAGE = "UTF-8";
	
	public static final boolean WRITE_TEMP_FILE = true;
	
	private static class Parameter
	{
		public String name;
		public String value;
		
		Parameter(String name, String value) {this.name = name; this.value = value;}
	}
	
	
	public static void main(String[] args) {
		
		System.setProperty("user.dir", EXECUTION_PATH);
		if(args.length == 4)
		{
			
			System.out.println("Use profile and Rules Database to compute Schematron's Rules File ...");
			String rules = computeSchematronRule(args[0], args[1]);

			
			System.out.println("Generate SVRL Report ...");
			String svrl_result = schematronValidation(args[2], new StreamSource(new StringReader(rules)));

			
			System.out.println("Create Html Report ...");
			generateHTML(svrl_result, args[2], args[1], args[3]);
			
			System.out.println("Validation is completed successfully");
		}
		else System.out.println("java -jar ARCEngine.jar profil rulesAIXM data report");
		/*
		String rules = computeSchematronRule("../NDBX/ARC-WEB/profile/NDBX_1.4_NDBX.xml", "../NDBX/ARC-WEB/data/RulesDataBase.xml");
		String svrl_result = schematronValidation("../NDBX/ARC-WEB/ndbx/NDBX_Template.xml", new StreamSource(new StringReader(rules)));
		
		generateHTML(svrl_result, "../NDBX/ARC-WEB/ndbx/NDBX_Template.xml", 
				"../NDBX/ARC-WEB/data/RulesDataBase.xml", "../NDBX/ARC-WEB/html/REPORT-NDBX_Data.html");*/
	}
	
	/**
	 * Compute the schematron rules from AIXM file
	 * @param profilFile
	 * @param ruleAIXMFile
	 * @param outFile
	 */
	public static void computeSchematronRule(String profilFile, String ruleAIXMFile, String outFile)
	{
		writeInFile(computeSchematronRule(profilFile, ruleAIXMFile), outFile);
	}
	
	/**
	 * Compute the schematron rules from AIXM file
	 * @param profilFile
	 * @param ruleAIXMFile
	 * @return
	 */
	private static String computeSchematronRule(String profilFile, String ruleAIXMFile)
	{		
		String schematronRule = "";
		try {
			String computedProfil = executeXSLTSaxon(new StreamSource(new File(profilFile)),  new StreamSource(new File(PROFIL_CONSTRUCTOR_FILE)));
			String computedRule = executeXSLTSaxon(new StreamSource(new InputStreamReader (new FileInputStream (ruleAIXMFile), "UTF-8")),  new StreamSource(new StringReader(computedProfil)));
			schematronRule = executeXSLTSaxon(new StreamSource(new StringReader(computedRule)),  new StreamSource(new InputStreamReader (new FileInputStream (GENERATE_SCHEMA_FILE), "UTF-8")));
			
			if(WRITE_TEMP_FILE)
			{
				writeInFile(computedProfil, TEMP_DIRECTORY + COMPUTED_PROFIL_FILE);
				writeInFile(computedRule, TEMP_DIRECTORY + COMPUTED_RULE_FILE);
				writeInFile(schematronRule, TEMP_DIRECTORY + SCHEMATRON_RULE_FILE);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		
		return schematronRule;
	}
	
	/**
	 * Validate an xml file with schematron rules
	 * @param dataFile
	 * @param schematronRuleFile
	 * @param outFile
	 */
	public static void schematronValidation(String dataFile, String schematronRuleFile, String outFile)
	{				
		writeInFile(schematronValidation(dataFile, new StreamSource(new File(schematronRuleFile))), outFile);
	}
	
	/**
	 * Validate an xml file with schematron rules
	 * @param dataFile
	 * @param schematronRuleFile
	 * @param outFile
	 */
	private static String schematronValidation(String dataFile, StreamSource schematronRule)
	{	
		String SVRL_Result = "";
		try {
			
			String schematronValidator = executeXSLTSaxon(schematronRule,  new StreamSource(new InputStreamReader (new FileInputStream (SCHEMATRON_ENGINE_FILE), "UTF-8")));
			SVRL_Result = executeXSLTSaxon(new StreamSource(new InputStreamReader (new FileInputStream(dataFile), "UTF-8")),  new StreamSource(new StringReader(schematronValidator)));
			
			if(WRITE_TEMP_FILE)
			{
				writeInFile(schematronValidator, TEMP_DIRECTORY + SCHEMATRON_VALIDATOR_FILE);
				writeInFile(SVRL_Result, TEMP_DIRECTORY + SVRL_FILE);
			}
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return SVRL_Result;
	}
	
	/**
	 * Generate HTML data from SVRL
	 * @param dataFile
	 * @param schematronRuleFile
	 * @param outFile
	 */
	private static void generateHTML(String svrlResult, String dataFile, String dataAIXMFile, String htmlFile)
	{
		try {
			
			String getError = executeXSLTSaxon(new StreamSource(new StringReader(svrlResult)), new StreamSource(new InputStreamReader (new FileInputStream(GET_ERROR_ENGINE), "UTF-8")));
			String retreiveOnlyError = executeXSLTSaxon(new StreamSource(new InputStreamReader (new FileInputStream(dataFile), "UTF-8")), new StreamSource(new StringReader(getError)));
			// String errorTable = executeXSLTSaxon(new StreamSource(new StringReader(retreiveOnlyError)), new StreamSource(new InputStreamReader (new FileInputStream(ERROR_TO_TABLE), "UTF-8")));
			
			writeInFile(getError, TEMP_DIRECTORY + GET_ERROR_FILE);
			writeInFile(retreiveOnlyError, TEMP_DIRECTORY + RETREIVE_ONLY_ERROR_FILE);
			// writeInFile(errorTable, TEMP_DIRECTORY + ERROR_TABLE_FILE);
			
			// Create XSLT parameters table
			ArrayList<Parameter> parameters = new ArrayList<Parameter>();
			parameters.add(new Parameter("docLocation", new File(dataAIXMFile).toURI().toString()));
			parameters.add(new Parameter("errLocation", new File(TEMP_DIRECTORY + RETREIVE_ONLY_ERROR_FILE).toURI().toString()));
			executeXSLTSaxonToHTML(new StreamSource(new StringReader(svrlResult)), new StreamSource(new InputStreamReader (new FileInputStream(SVRL_TO_HTML), "UTF-8")), htmlFile, parameters);
		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Execute a SAXON XSLT2 Tranformation 
	 * @param xml
	 * @param xslt
	 * @return xml
	 */
	private static String executeXSLTSaxon(Source xml, Source xslt)
	{
		try {
			Processor proc = new Processor(false);
			XsltCompiler comp = proc.newXsltCompiler();
			XsltExecutable exp;
		
			exp = comp.compile(xslt);
		
	        XdmNode source = proc.newDocumentBuilder().build(xml);
	        XsltTransformer trans = exp.load();
	        trans.setInitialContextNode(source);    
	        XdmDestination resultTree = new XdmDestination();
	        trans.setDestination(resultTree);
	        trans.transform();

            return "<?xml version=\"1.0\" encoding=\"" + ENCODAGE + "\"?>\n" + resultTree.getXdmNode().toString();
            
		} catch (SaxonApiException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Execute a SAXON XSLT2 Tranformation 
	 * @param xml
	 * @param xslt
	 * @return html
	 */
	private static String executeXSLTSaxonToHTML(Source xml, Source xslt, String outPutFile, ArrayList<Parameter> parameters)
	{
		try {
			Processor proc = new Processor(false);
			XsltCompiler comp = proc.newXsltCompiler();
			XsltExecutable exp;
		
			exp = comp.compile(xslt);
		
	        XdmNode source = proc.newDocumentBuilder().build(xml);
	        
	        Serializer out = new Serializer();
            out.setOutputProperty(Serializer.Property.METHOD, "html");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");
            out.setOutputFile(new File(outPutFile));
            
	        XsltTransformer trans = exp.load();
	        trans.setInitialContextNode(source);
	        
	        for (Parameter parameter : parameters) {
	        	trans.setParameter(new QName(parameter.name), new XdmAtomicValue(parameter.value));
			}
            trans.setDestination(out);
            trans.transform();
            
		} catch (SaxonApiException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Execute a SAXON XSLT2 Tranformation 
	 * @param xml
	 * @param xslt
	 * @param out
	 */
	public static void executeXSLTSaxon(String xmlFile, String xsltFile, String outFile)
	{
		writeInFile(executeXSLTSaxon(new StreamSource(new File(xmlFile)), new StreamSource(new File(xsltFile))), outFile);
	}
	
	/**
	 * Write in file
	 * @param toWrite
	 * @param outFile
	 */
	private static void writeInFile(String toWrite, String outFile)
	{
		try {
			FileWriter fw = new FileWriter(outFile);
			
			fw.write(toWrite);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
