package com.capgemini.kabanos.gatherer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.capgemini.kabanos.common.configuration.Configuration;
import com.capgemini.kabanos.common.domain.Implementation;
import com.capgemini.kabanos.common.domain.Preposition;
import com.capgemini.kabanos.common.domain.Test;
import com.capgemini.kabanos.common.domain.TestFile;
import com.capgemini.kabanos.common.enums.LanguageType;
import com.capgemini.kabanos.common.utility.ArrayUtils;
import com.capgemini.kabanos.common.utility.FileUtils;
import com.capgemini.kabanos.common.utility.StringUtils;
import com.capgemini.kabanos.database.DataBase;
import com.capgemini.kabanos.gatherer.extractor.IExtractor;
import com.capgemini.kabanos.gatherer.extractor.JavaExtractor;

public class Gatherer {

	// parallel processing not always gives benefit. if the array is to small then
	// parallel is not needed
	private final int PARALLEL_ARRAY_SIZE_THRESHOLD = 10;
	private IExtractor extractor = null;

	public List<List<Preposition>> gatherKnowledge(LanguageType languageType, String[] paths) throws Exception {

		switch (languageType) {
		case JAVA:
			extractor = new JavaExtractor();
			break;
		default:
			throw new Exception("Unsuported language type: " + languageType);
		}

		Set<String> uniquePaths = new HashSet<>();
		for (String path : paths)
			uniquePaths.addAll(FileUtils.listFiles(path));

		List<TestFile> testFiles = uniquePaths.stream()
				.map(filePath -> extractor.extractFunctionsFromTestFile(filePath)).collect(Collectors.toList());

		return createImplementationPrepositionList(testFiles);
	}

	/*
	 * list of prepositions in a list of tests
	 */
	private List<List<Preposition>> createImplementationPrepositionList(List<TestFile> testFiles) {
		Stream<TestFile> stream = testFiles.size() > PARALLEL_ARRAY_SIZE_THRESHOLD ? testFiles.parallelStream()
				: testFiles.stream();

		List<List<Preposition>> result = stream.map(this::extractPrepositionsFromTestFile).reduce(ArrayUtils::megre)
				.orElse(new ArrayList<>());

		return result;
	}

	private List<List<Preposition>> extractPrepositionsFromTestFile(TestFile file) {
		return file.getTests().stream().map(this::extractPrepositionsFromTest).filter(el -> el.size() > 0)
				.collect(Collectors.toList());
	}

	private List<Preposition> extractPrepositionsFromTest(Test t) {
		List<Preposition> result = new ArrayList<Preposition>();

		Map<String, Preposition> prepositionMap = new HashMap<>();

		String loggerLine = "";
		boolean foundLoggerStart = false, foundLoggerEnd = false;

		Preposition preposition = null;
		Preposition predecessor = null;
		Implementation implementation = new Implementation();

		// TODO ladniej!!!!!!!!!!!!!!!!!
		String loggerPrefix = Configuration.INSTANCE.getFilesConfiguration().getLoggerPrefix();
		String loggerSuffix = Configuration.INSTANCE.getFilesConfiguration().getLoggerSuffix();
		String loggerStepRegex = Configuration.INSTANCE.getFilesConfiguration().getLoggerStepRegex();

		for (String line : t.getLines()) {
			line = line.trim();

			if (foundLoggerEnd && !line.startsWith(loggerPrefix))
				if (line.length() > 0) {
					implementation.addLine(line);
				}

			// multiline logger (more that 2 lines)
			if (foundLoggerStart && !foundLoggerEnd && !line.endsWith(loggerSuffix)) {
				loggerLine += line;
			}

			// single line logger
			else if (line.startsWith(loggerPrefix)) {
				foundLoggerStart = true;
				loggerLine = line;
				foundLoggerEnd = line.endsWith(loggerSuffix);

				if (preposition != null) {
					if (implementation != null)
						preposition.addImplementation(implementation);
					result.add(preposition);
				}

				if (foundLoggerEnd) {
					predecessor = preposition;

					String key = this.extractLoggerText(loggerLine, loggerPrefix, loggerSuffix, loggerStepRegex);
					preposition = this.getPreposition(key, prepositionMap);

					preposition.addPredecessor(predecessor);
					implementation = new Implementation();
				}
			}

			else if (foundLoggerStart && !foundLoggerEnd && line.endsWith(loggerSuffix)) {
				loggerLine += line;
				foundLoggerEnd = true;

				if (preposition != null)
					result.add(preposition);

				predecessor = preposition;

				String key = this.extractLoggerText(loggerLine, loggerPrefix, loggerSuffix, loggerStepRegex);
				preposition = this.getPreposition(key, prepositionMap);

				preposition.addPredecessor(predecessor);
				implementation = new Implementation();
			}
		}

		if (preposition != null) {
			if (implementation != null)
				preposition.addImplementation(implementation);
			result.add(preposition);
		}

		return result;
	}

	private Preposition getPreposition(String key, Map<String, Preposition> prepositionMap) {
		Preposition preposition;
		String formattedKey = StringUtils.formatLoggerStep(key);
		if (prepositionMap.containsKey(formattedKey)) {
			preposition = prepositionMap.get(formattedKey);
		} else {
			preposition = new Preposition(key, formattedKey);
			prepositionMap.put(formattedKey, preposition);
		}
		return preposition;
	}

	public String extractLoggerText(String line, String prefix, String sufix, String loggerStepRegex) {
		line = line.trim();

		if (line.startsWith(prefix))
			line = line.replace(prefix, "");

		if (line.endsWith(sufix))
			line = line.substring(0, line.length() - sufix.length());

		line = line.trim();

		if (line.matches("^" + loggerStepRegex + ".*")) {
			Pattern p = Pattern.compile("^" + loggerStepRegex);
			Matcher m = p.matcher(line);
			line = m.replaceFirst("").trim();
		}

		return line;
	}

	public boolean saveKnowledge(List<Preposition> knowledge) {
		return new DataBase().savePrepositions(knowledge);
	}
}