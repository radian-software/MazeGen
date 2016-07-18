package mazes.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import mazes.gen.GrowingTreeMazeGenerator;
import mazes.schematic.Dimensions;
import mazes.schematic.Piece;
import mazes.svg.SVGDocument;
import util.ArrayUtil;
import util.Triplet;

public final class MazeIO {
	
	private static class ExceptionContainer {
		public Throwable e;
	}
	
	public static final Scanner scanner = new Scanner(System.in);
	
	private MazeIO() {}
	
	public static GrowingTreeMazeGenerator loadMaze(String mazeName, boolean printErrors) {
		ExceptionContainer e1 = new ExceptionContainer(), e2 = new ExceptionContainer();
		GrowingTreeMazeGenerator gen;
		gen = loadMaze(mazeName, "mazes/", e1);
		if (gen != null) return gen;
		gen = loadMaze(mazeName, "../mazes/", e2);
		if (gen != null) return gen;
		if (printErrors) {
			System.out.printf("Received errors while trying to load maze '%s':%n", mazeName);
			e1.e.printStackTrace(System.out);
			e2.e.printStackTrace(System.out);
		}
		return null;
	}
	private static GrowingTreeMazeGenerator loadMaze(String mazeName, String pathName, ExceptionContainer error) {
		try (
				InputStream file = new FileInputStream(pathName + mazeName);
				InputStream buffer = new BufferedInputStream(file);
				ObjectInput input = new ObjectInputStream(buffer);
				) {
			return (GrowingTreeMazeGenerator) input.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			error.e = e;
			return null;
		}
	}
	
	public static boolean saveMaze(GrowingTreeMazeGenerator gen, String mazeName, boolean printErrors) {
		Throwable e1, e2;
		if ((e1 = saveMaze(gen, mazeName, "mazes/")) != null && (e2 = saveMaze(gen, mazeName, "../mazes/")) != null) {
			if (printErrors) {
				System.out.printf("Received errors while trying to save maze to '%s':%n", mazeName);
				e1.printStackTrace(System.out);
				e2.printStackTrace(System.out);
			}
			return false;
		}
		else return true;
	}
	private static Throwable saveMaze(GrowingTreeMazeGenerator gen, String mazeName, String pathName) {
		if (new File(pathName + mazeName).exists()) {
			return new IOException("file already exists");
		}
		try (
				OutputStream file = new FileOutputStream(pathName + mazeName);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);
				) {
			output.writeObject(gen);
			return null;
		}
		catch (IOException e) {
			return e;
		}
	}
	
	public static boolean fileExists(String mazeName) {
		return fileExists(mazeName, "mazes/") || fileExists(mazeName, "../mazes/");
	}
	private static boolean fileExists(String mazeName, String pathName) {
		return new File(pathName + mazeName).exists();
	}
	
	public static boolean saveSVG(List<SVGDocument> documents, int[] mazeSize, String mazeName, boolean printErrors) {
		Throwable e1, e2;
		if ((e1 = saveSVG(documents, mazeSize, mazeName, "mazes-svg/")) != null && (e2 = saveSVG(documents, mazeSize, mazeName, "../mazes-svg/")) != null) {
			if (printErrors) {
				System.out.printf("Received errors while trying to save maze to '%s':%n", mazeName);
				e1.printStackTrace(System.out);
				e2.printStackTrace(System.out);
			}
			return false;
		}
		else return true;
	}
	private static Throwable saveSVG(List<SVGDocument> documents, int[] mazeSize, String mazeName, String pathName) {
		File folder = new File(pathName + mazeName);
		if (folder.exists()) {
			return new IOException("folder already exists");
		}
		if (!folder.mkdir()) {
			return new IOException("could not create folder");
		}
		int i = 0;
		for (SVGDocument document : documents) {
			document.getDocumentation().sort((t1, t2) -> {
				return t1.getSecond().compareTo(t2.getSecond());
			});
		}
		// The order in which the pieces are listed in the key.
		List<Piece> keyOrderPieces = documents.stream().flatMap(
				document -> document
					.getDocumentation()
					.stream()
					.<Piece>map(Triplet::getFirst))
				.collect(Collectors.toList());
		// The order in which we assemble the pieces.
		List<Piece> assemblyOrderPieces = Piece.orderPieces(keyOrderPieces, mazeSize);
		for (SVGDocument document : documents) {
			document.setLineWidth(Dimensions.laserLineWidth());
			String svgCode = document.toSVGCode().stream().collect(Collectors.joining("\n"));
			try (PrintWriter writer = new PrintWriter(String.format("%s%s/page%03d.svg", pathName, mazeName, i + 1))) {
				writer.print(svgCode);
			}
			catch (FileNotFoundException e) {
				return e;
			}
			document.setLineWidth(Dimensions.debugLineWidth());
			document.addLabels(documents, mazeSize);
			String newSvgCode = document.toSVGCode().stream().collect(Collectors.joining("\n"));
			try (PrintWriter writer = new PrintWriter(String.format("%s%s/map%03d.svg", pathName, mazeName, i + 1))) {
				writer.print(newSvgCode);
			}
			catch (FileNotFoundException e) {
				return e;
			}
			
			String key = document.getDocumentation().stream().map(
					doc -> String.format("[Ordinal piece #%d]%n[Key piece #%d]%n%s",
							assemblyOrderPieces.indexOf(doc.getFirst()) + 1, keyOrderPieces.indexOf(doc.getFirst()) + 1, doc.getThird())).collect(Collectors.joining("\n"));
			try (PrintWriter writer = new PrintWriter(String.format("%s%s/key%03d.txt", pathName, mazeName, i + 1))) {
				writer.print(key);
			}
			catch (FileNotFoundException e) {
				return e;
			}
			i += 1;
		}
		List<String> docStrings = documents.stream().flatMap(
			document -> document
				.getDocumentation()
				.stream()
				.<String>map(triplet -> {
					return String.format("[DOCUMENT %03d]%n[Ordinal piece #%d]%n[Key piece #%d]%n%s", documents.indexOf(document) + 1, assemblyOrderPieces.indexOf(triplet.getFirst()) + 1, keyOrderPieces.indexOf(triplet.getFirst()) + 1, triplet.getThird());
				}))
			.collect(Collectors.toList());
		ArrayUtil.parallelSort(keyOrderPieces, (p1, p2) -> assemblyOrderPieces.indexOf(p1) - assemblyOrderPieces.indexOf(p2), docStrings);
		String orderedDocumentation = docStrings.stream().collect(Collectors.joining("\n"));
		try (PrintWriter writer = new PrintWriter(String.format("%s%s/instructions.txt", pathName, mazeName))) {
			writer.print(orderedDocumentation);
		}
		catch (FileNotFoundException e) {
			return e;
		}
		return null;
	}
	
	public static String intermeshLines(String left, String right, String delimiter, String separator) {
		String[] leftLines = left.split(Pattern.quote(delimiter));
		String[] rightLines = right.split(Pattern.quote(delimiter));
		if (leftLines.length != rightLines.length) throw new IllegalArgumentException();
		String[] combinedLines = new String[leftLines.length];
		for (int i=0; i<leftLines.length; i++) {
			combinedLines[i] = leftLines[i] + separator + rightLines[i];
		}
		return String.join(delimiter, combinedLines);
	}
	
}
