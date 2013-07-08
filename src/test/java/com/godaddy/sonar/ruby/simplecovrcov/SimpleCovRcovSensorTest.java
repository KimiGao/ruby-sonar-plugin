package com.godaddy.sonar.ruby.simplecovrcov;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import com.godaddy.sonar.ruby.core.RubyFile;
import com.godaddy.sonar.ruby.simplecov.SimpleCovCsvFileResult;
import com.godaddy.sonar.ruby.simplecov.SimpleCovCsvParser;
import com.godaddy.sonar.ruby.simplecov.SimpleCovCsvParserImpl;
import com.godaddy.sonar.ruby.simplecov.SimpleCovCsvResult;

public class SimpleCovRcovSensorTest 
{
	private static String RESULT_CSV_FILE = "src/test/resources/test-data/results.csv";
	private static String RESULT_JSON_FILE = "src/test/resources/test-data/results.json";
	private static String RESULT_NO_TESTS_JSON_FILE = "src/test/resources/test-data/results-zero-tests.json";
	
	private IMocksControl mocksControl;
	private Settings settings;
	private ModuleFileSystem moduleFileSystem;
	private SimpleCovRcovJsonParser simpleCovRcovJsonParser;
	private SimpleCovRcovSensor simpleCovRcovSensor;
	private SimpleCovCsvParser simpleCovCsvParser;
	private SensorContext sensorContext;
	
	@Before
	public void setUp() throws Exception
	{
		mocksControl = EasyMock.createControl();
		settings = new Settings();
		moduleFileSystem = mocksControl.createMock(ModuleFileSystem.class);
		simpleCovRcovJsonParser = mocksControl.createMock(SimpleCovRcovJsonParser.class);
		simpleCovCsvParser = mocksControl.createMock(SimpleCovCsvParser.class);
		
		simpleCovRcovSensor = new SimpleCovRcovSensor(settings, moduleFileSystem, simpleCovRcovJsonParser, simpleCovCsvParser);
	}
	
	@Test
	public void testConstructor() 
	{	
		assertNotNull(simpleCovRcovSensor);
	}
	
	@Test
	public void testShouldExecuteOnRubyProject()
	{
		Configuration config = mocksControl.createMock(Configuration.class);
		expect(config.getString("sonar.language", "java")).andReturn("ruby");
		mocksControl.replay();
		
		Project project = new Project("test project");
		project.setConfiguration(config);
		
		assertTrue(simpleCovRcovSensor.shouldExecuteOnProject(project));
		
		mocksControl.verify();		
	}
	
	@Test
	public void testShouldNotExecuteOnJavascriptProject()
	{
		Configuration config = mocksControl.createMock(Configuration.class);
		expect(config.getString("sonar.language", "java")).andReturn("javascript");
		mocksControl.replay();
		
		Project project = new Project("test project");
		project.setConfiguration(config);
		
		assertFalse(simpleCovRcovSensor.shouldExecuteOnProject(project));
		
		mocksControl.verify();		
	}
	
	@Test
	public void testAnalyse() throws IOException
	{
		SimpleCovCsvResult results = new SimpleCovCsvParserImpl().parse(new File(RESULT_CSV_FILE));
		Map<String, CoverageMeasuresBuilder> jsonResults = new SimpleCovRcovJsonParserImpl().parse(new File(RESULT_JSON_FILE));
		
		List<File> sourceDirs = new ArrayList<File>();
		
		for (SimpleCovCsvFileResult result : results.getCsvFilesResult())
		{
			String fileName = result.getFileName().replaceAll("\"", "");
			sourceDirs.add(new File(fileName));
		}
		
		Measure measure = new Measure();
			
		sensorContext = mocksControl.createMock(SensorContext.class);
		expect(moduleFileSystem.sourceDirs()).andReturn(sourceDirs).once();
		expect(simpleCovCsvParser.parse(eq(new File("coverage/results.csv")))).andReturn(results).once();
		expect(simpleCovRcovJsonParser.parse(eq(new File("coverage/.resultset.json")))).andReturn(jsonResults).once();
		expect(sensorContext.saveMeasure(eq(CoreMetrics.COVERAGE), eq(results.getTotalPercentCoverage()))).andReturn(measure).once();
		expect(sensorContext.saveMeasure(isA(RubyFile.class), isA(Metric.class), isA(Double.class))).andReturn(measure).times(48);
		expect(sensorContext.saveMeasure(isA(RubyFile.class), isA(Measure.class))).andReturn(measure).times(12);
		//expect(sensorContext.saveMeasure(isA(org.sonar.api.resources.File.class), isA(Measure.class))).andReturn(measure).times(12);
		
		mocksControl.replay();

		simpleCovRcovSensor.analyse(new Project("key_name"), sensorContext);
		
		mocksControl.verify();		
	}
}
