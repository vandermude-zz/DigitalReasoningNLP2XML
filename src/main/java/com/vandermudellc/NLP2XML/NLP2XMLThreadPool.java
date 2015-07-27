package NLP2XML;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 
 * @author Antony Van der Mude
 * 
 *         Natural Language Processing To XML format with Named Entity
 *         Recognition. Reads in a zip file and uses a thread pool to
 *         parallelize the processing of the text files contained in the zip.
 *         Finds the text files and processes each in a separate thread. If the
 *         input file is xxx.txt the output goes to xxx.xml and xxx.ner for
 *         aggregation later.
 */
public class NLP2XMLThreadPool {
	public static void main(String[] args) {
		String inputFileName = null;
		String posFileName = null;
		String aggregateOutputFileName = null;
		String aggregateNERFileName = null;
		List<String> outputFileNames = new ArrayList<String>();
		List<String> nerFileNames = new ArrayList<String>();
		// No fancy option stuff, just looking for -p or --pos options
		if (args.length < 1) {
			System.err.println("ERROR: Input file name not given");
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p") || args[i].equals("--pos")) {
				i++;
				posFileName = args[i];
				System.out.format("PartsOfSpeech: %s\n", posFileName);
			} else if (inputFileName == null) {
				inputFileName = args[i];
			} else if (aggregateOutputFileName == null) {
				aggregateOutputFileName = args[i];
			} else if (aggregateNERFileName == null) {
				aggregateNERFileName = args[i];
			}
		}
		System.out.format("Input Zip file=%s\n", inputFileName);
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newCachedThreadPool();
		Collection<Future<?>> futures = new LinkedList<Future<?>>();
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(inputFileName);
		} catch (IOException e) {
			System.err.format("ERROR: NLP2XMLThreadPool IOException=%s\n",
					e.getMessage());
			e.printStackTrace();
		}
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			File inputFile = new File(inputFileName);
			String inputDirectory = inputFile.getParent();
			File readFile = new File(entry.getName());
			String readFileName = readFile.getName();
			if (readFileName.startsWith(".") || !readFileName.endsWith(".txt")) {
				System.out.format("SKIP %s\n", entry.getName());
				continue;
			}
			if (readFileName.indexOf(".") > 0) {
				readFileName = readFileName.substring(0,
						readFileName.lastIndexOf("."));
			}
			try {
				InputStream stream = zipFile.getInputStream(entry);
				String taskName = entry.getName();
				String outputFileName = inputDirectory + "/" + readFileName
						+ ".xml";
				outputFileNames.add(outputFileName);
				String nerFileName = inputDirectory + "/" + readFileName
						+ ".ner";
				nerFileNames.add(nerFileName);
				System.out.format("A new task has been added: %s -> %s, %s\n",
						taskName, outputFileName, nerFileName);
				NLP2XMLTask task = new NLP2XMLTask(stream, taskName,
						outputFileName, nerFileName, posFileName);
				futures.add(executor.submit(task));
			} catch (IOException e) {
				System.err.format("ERROR: NLP2XMLThreadPool IOException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				System.err.format(
						"ERROR: NLP2XMLThreadPool InterruptedException=%s\n",
						e.getMessage());
				e.printStackTrace();
			} catch (ExecutionException e) {
				System.err.format(
						"ERROR: NLP2XMLThreadPool ExecutionException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
		executor.shutdown();
		System.out.format("Concatenate to %s\n", aggregateOutputFileName);
		concatenateFiles(aggregateOutputFileName, outputFileNames);
		System.out.format("Concatenate to %s\n", aggregateNERFileName);
		concatenateFiles(aggregateNERFileName, nerFileNames);
		System.out.format("DONE\n");
	}

	/**
	 * Takes list of output file from different tasks and aggregate file name.
	 * Concatenate the files together into a single aggregate file.
	 * 
	 * @param aggregateFileName
	 * @param fileNames
	 */
	static void concatenateFiles(String aggregateFileName,
			List<String> fileNames) {
		PrintStream out = System.out;
		if (aggregateFileName != null) {
			try {
				out = new PrintStream(aggregateFileName);
			} catch (FileNotFoundException e) {
				System.err.format(
						"ERROR: concatenateFiles FileNotFoundException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
		for (String fileName : fileNames) {
			out.print("FILE:" + fileName + "\n");
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(fileName));
				String line;
				while ((line = br.readLine()) != null) {
					out.print(line + "\n");
				}
			} catch (FileNotFoundException e) {
				System.err.format(
						"ERROR: concatenateFiles FileNotFoundException=%s\n",
						e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.format("ERROR: concatenateFiles IOException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
		out.close();
	}

}
