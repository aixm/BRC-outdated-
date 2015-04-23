/**
 *	Project:       NDBX Business Rules Review <BR>
 *	Owner:         EUROCONTROL, Rue de la Fusee, 96, B-1130 Brussels, Belgium
 * 	Provider:      AEROCONSEIL
 *  Title:         ARC Extension functions
 *  Contacts:
 *  E.POROSNICU for EUROCONTROL
 *  H.LEPORI for AEROCONSEIL
 *  P.KARP for EURILOGIC
*/
package com.aeroconseil;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.xml.sax.InputSource;

import com.aeroconseil.geolib.Angle;
import com.aeroconseil.geolib.GeographicCoordinate;
import com.aeroconseil.geolib.Geographic_Exception;
import com.aeroconseil.geolib.Latitude;
import com.aeroconseil.geolib.Longitude;
import com.aeroconseil.geolib.Point;

/**
 *	ARC Web tool extension functions. <BR><BR>
 *  
 *  These functions are used in the schematron rules for extend XSLT engine capabilities. 
 *  They have only be tested with Saxon 9 XSLT engine. 
 *  Before calling the function, add the namespace declaration in XSLT file (xmlns:arcext="java:com.aeroconseil.ARCExtFunctions")
 *  or in schematron file (&lt;ns prefix="arcext" uri="java:com.aeroconseil.ARCExtFunctions"/&gt;).
 *  For more information about Saxon Java extensibility see <a href="http://www.saxonica.com/documentation/extensibility/functions.html"> Saxon Extensibility<a>
 *	<BR><BR>
 *  Developed by AEROCONSEIL for <a href="mailto:eduard.porosnicu@eurocontrol.int">EUROCONTROL<a><BR>This work is subject to the license provided in the file LICENSE.txt<BR>
 *  @author P.KARP
 *  @version 1.0
 *  
*/
public class ARCExtFunctions {

	private static final String EXECUTION_PATH = (ARCEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath().contains(".jar")) ? ARCEngine.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("ARCEngine.jar", "") : "./";
		
	/**
	 * Round half down at decimal
	 * @param d 			Decimal number to round
	 * @param decimalPlace 	Number of digits after the decimal point
	 * @return Ronded decimal value
	 */
	public static double round(double d, int decimalPlace) {
	    BigDecimal bd = new BigDecimal(Double.toString(d));
	    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_DOWN);
	    return bd.doubleValue();
	}
	
	/**
	 * Add the value to the angle 
	 * @param angle 			Range [0, 360]
	 * @param additionValue 	Value to add
	 * @return angle Range [0, 360]
	 */
	public static double additionAngle(double angle, double additionValue)
	{
		try {
			return new Angle(angle).addition(additionValue).getValue();
		} catch (Geographic_Exception e) {
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	/**
	 * Subtract the value to the angle 
	 * @param angle 			Range [0, 360]
	 * @param substractValue  	Value to subtract
	 * @return angle Range [0, 360]
	 */
	public static double substractAngle(double angle, double substractValue)
	{
		try {
			return new Angle(angle).subtraction(substractValue).getValue();
		} catch (Geographic_Exception e) {
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	/**
	 * Get the difference between two courses
	 * @param inputCourse 	angle range [0, 360] 
	 * @param outputCourse	angle range [0, 360]
	 * @return value range [0, 180]
	 */
	public static double courseDiff(double inputCourse, double outputCourse)
	{
		
		try {
			Angle out = new Angle(outputCourse).resize(true);
			Angle result = new Angle(inputCourse).resize(true).subtraction(out);
			return Math.abs(result.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	/**
	 * Get the course between two points
	 * @param latlong1	Lat/long of the first point at format : "10.1 -5.0"
	 * @param latlong2	Lat/long of the second point at format : "10.1 -5.0"
	 * @return angle 	range [0, 360]
	 */
	public static double courseBetweenPoints(String latlong1, String latlong2)
	{
		
		try {
			String[] latlong1Tab = latlong1.split(" ");
			String[] latlong2Tab = latlong2.split(" ");
			Point point1 = new Point(new Latitude(Double.parseDouble(latlong1Tab[0])), new Longitude(Double.parseDouble(latlong1Tab[1])));
			Point point2 = new Point(new Latitude(Double.parseDouble(latlong2Tab[0])), new Longitude(Double.parseDouble(latlong2Tab[1])));

			return point1.directCourseTo(point2).getValue();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Double.NaN;
	}
	
	/**
	 * Check the ICAO - AREA consistency
	 * The table is given in the XML file "ICAO_Table.xml" located in the execution path :
	 *  <BR>&lt;ICAO_Table&gt; <BR>
	 *  	&lt;ICAO name="AG"&gt;<BR>
	 *  		&lt;Area name="SPA"/&gt;<BR>
	 *		&lt;/ICAO&gt;<BR>
	 *		&lt;ICAO name="AN"&gt;<BR>
	 *  		&lt;Area name="SPA"/&gt;<BR>
	 *		&lt;/ICAO&gt;<BR>
	 *	...<BR>
	 *	&lt;/ICAO_Table&gt;
	 * @param icao code (2 characters)
	 * @param area code (3 characters)
	 * @return true if ICAO and AREA are consistent else false
	 */
	public static boolean isConsistentICAO_AREA(String icao, String area)
	{
		String resultString = "";
		
		try {
			
			// Load ICAO table
			// The file "ICAO_Table.xml" must be in the same execution path			
			InputSource source = new InputSource(new FileInputStream(new File(EXECUTION_PATH + "ICAO_Table.xml")));
            
            // Create XPath 
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            	        
	        // Compile the expression
            XPathExpression xpathExpression = xpath.compile("boolean(/ICAO_Table/ICAO[@name = '" + icao + "']/Area[@name = '" + area + "'])");
            
            // Evaluate the expression on the document to get String result
            resultString = xpathExpression.evaluate(source);
            
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultString.equals("true");
	}
	
	/**
	 * Check if 2 fixes are collocated : the lat/longs tolerance is 1/10 arc minutes
	 * @param latlong1 - Lat/long of the first navaid at format : "10.1 -5.0"
	 * @param latlong2 - Lat/long of the second navaid at format : "10.1 -5.0"
	 * @return Return true if difference between latlongs is less than 1/10 arc minutes
	 */
	public static boolean collocated(String latlong1, String latlong2)
	{
		
		try {
			// Tolerance is 1/10 arc minutes
			GeographicCoordinate tolerance = new GeographicCoordinate("E000-00-06.00", "PDDD-MM-SS.ss");
			
			// Parse to Point
			String[] latlong1Tab = latlong1.split(" ");
			String[] latlong2Tab = latlong2.split(" ");
			Point point1 = new Point(new Latitude(Double.parseDouble(latlong1Tab[0])), new Longitude(Double.parseDouble(latlong1Tab[1])));
			Point point2 = new Point(new Latitude(Double.parseDouble(latlong2Tab[0])), new Longitude(Double.parseDouble(latlong2Tab[1])));
			
			// Compare position
			return point1.equals(point2, tolerance.getValue());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Get the distance between two points
	 * @param latlong1		Lat/long of the first point at format : "10.1 -5.0"
	 * @param latlong2 		Lat/long of the second point at format : "10.1 -5.0"
	 * @return	decimal distance value in Nautical Mile (NM)
	 */
	public static double distanceBetweenPoints(String latlong1, String latlong2)
	{
		try {
			String[] latlong1Tab = latlong1.split(" ");
			String[] latlong2Tab = latlong2.split(" ");
			Point point1 = new Point(new Latitude(Double.parseDouble(latlong1Tab[0])), new Longitude(Double.parseDouble(latlong1Tab[1])));
			Point point2 = new Point(new Latitude(Double.parseDouble(latlong2Tab[0])), new Longitude(Double.parseDouble(latlong2Tab[1])));

			return point1.distanceTo(point2);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return Double.NaN;
	}
	
	/**
	 * Get the opposite runway of the runway
	 * Ex. RW22L -> RW04R
	 * @param runwayID	The runway ID at format R [00 - 32] (L | R | M)
	 * @return The opposite Runway ID 
	 */
	public static String getOppositeRunway(String runwayID)
	{
		String opposite = "RW";
		
		// Invert the runway numeric value
		int numeric = Integer.parseInt(runwayID.substring(2, 4));
		
		if(numeric <= 18) numeric = numeric + 18;
		else numeric = numeric - 18;
		
		if(numeric > 9)
			opposite = opposite + numeric;
		else
			opposite = opposite + "0" + numeric;
				
		// Invert the side
		if(runwayID.length() > 4)
		{
			if(runwayID.charAt(4) == 'L')
				opposite = opposite + "R";
			else if(runwayID.charAt(4) == 'R')
				opposite = opposite + "L";
			else 
				opposite = opposite + runwayID.charAt(4);	
		}
		
		return opposite;
	}
	
	/**
	 * 	Get the MAP position beside the runway
	 * @param course			Final Approach course
	 * @param mapLatlong 		Lat/long of the MAP at format : "10.1 -5.0"
	 * @param runwayLatlong 	Lat/long of the runway at format : "10.1 -5.0"
	 * @return -1 if the MAP is prior
	 * 			0 if the MAP is abeam
	 * 			1 if the MAP is beyond 
	 */
	public static int MAP_position(double course, String mapLatlong, String runwayLatlong)
	{
		int result = Integer.MAX_VALUE;
		try {
			String[] mapLatlongTab = mapLatlong.split(" ");
			String[] runwayLatlongTab = runwayLatlong.split(" ");
			Point mapPoint = new Point(new Latitude(Double.parseDouble(mapLatlongTab[0])), new Longitude(Double.parseDouble(mapLatlongTab[1])));
			Point runwaypoint = new Point(new Latitude(Double.parseDouble(runwayLatlongTab[0])), new Longitude(Double.parseDouble(runwayLatlongTab[1])));

			double courseDiff = courseDiff(course, mapPoint.directCourseTo(runwaypoint).getValue());
			if(courseDiff < 90.1 && courseDiff > 89.9)
			{
				result = 0;
			}
			else if(courseDiff >= 90.1)
			{
				result = 1;
			}
			else if(courseDiff <= 89.9)
			{
				result = -1;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * Get XPath expression contains in the xlink expression
	 * @param xlinkExpr		XLink Expression
	 * @return xPath Expression
	 */
	public static String getXPath(String xlinkExpr)
	{
		String xPath = xlinkExpr.replace("#xpointer(", "");
		xPath = xPath.substring(0, xPath.length() - 1);
		return xPath;
	}
	
	/***
	 * Get the altitude difference between the from point and to point with the angle at from point
	 *       TO +  
	 *         /|
	 *        / |altitude
	 *       /\A|
	 *  FP  +__\|______
	 *    
	 * @param fromPoint Lat/long of the from point at format : "10.1 -5.0"
	 * @param angle range [0, 90]
	 * @param toPoint Lat/long of the to point at format : "10.1 -5.0"
	 * @return altitude in feet
	 */
	public static double getAltitudeDiff(String fromPoint, double angle, String toPoint)
	{
		// Compute the distance between fromPoint and toPoint
		double distance = distanceBetweenPoints(fromPoint, toPoint);
		
		// Set the distance in feet
		distance = distance * (2315000 / 381);
		
		double result = distance * Math.sin(Math.toRadians(angle));
		
		return result;
	}
	
	/**
	 * Convert the value to Nautical Mile
	 * @param value 	value to convert 
	 * @param uom 		Unit of measurement (AIXM uomDistanceType)
	 * @return value in Nautical Mile (NM)
	 */
	public static double convertToNM(double value, String uom)
	{
		double result = Double.NaN;
		
		if(uom.equals("NM")) 		// Nautical Mile
			result = value;
		else if(uom.equals("KM")) 	// Kilometer
			result = value / 1.852;
		else if(uom.equals("M")) 	// Meter
			result = value / 1852;
		else if(uom.equals("FT")) 	// Feet
			result = value / (2315000 / 381);
		else if(uom.equals("MI")) 	// Statute Mile
			result = value / (57.875 / 50.292);
		else if(uom.equals("OTHER"))// Other ? ->  No conversion
			result = value;
		
		return result;
	}
	
}
