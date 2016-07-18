package util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ArrayUtil {
	
	public static class NaturalOrdering<T> implements Comparator<T> {
		@SuppressWarnings("unchecked")
		@Override public int compare(T o1, T o2) {
			return ((Comparable<T>) o1).compareTo(o2);
		}
	}
	
	private ArrayUtil() {}
	
	public static <T, U> void parallelSort(List<T> sortBy, Comparator<T> c, List<U> list) {
		List<Pair<T, U>> combinedList = StreamUtil.<T, U, Pair<T, U>>zip(sortBy.stream(), list.stream(), Pair<T, U>::new).collect(Collectors.toList());
		final Comparator<T> comparator = c == null ? new NaturalOrdering<T>() : c;
		combinedList.sort((p1, p2) -> comparator.compare(p1.getFirst(), p2.getFirst()));
		setAll(sortBy, StreamUtil.toIterable(combinedList.stream().map(Pair::getFirst)));
		setAll(list, StreamUtil.toIterable(combinedList.stream().map(Pair::getSecond)));
	}
	
	public static <T> void setAll(List<? super T> dest, int index, Iterable<? extends T> src) {
		Iterator<? extends T> iterator = src.iterator();
		for (int i=index; i<dest.size(); i++) {
			dest.set(i, iterator.next());
		}
	}
	public static <T> void setAll(List<? super T> dest, Iterable<? extends T> src) {
		setAll(dest, 0, src);
	}
	
	public static String toCharMatrix(boolean[][] mat, String trueStr, String falseStr) {
		Objects.requireNonNull(mat);
		int columnLength = getColumnLength(mat);
		if (columnLength == -1) throw new IllegalArgumentException();
		return IntStream.range(0, columnLength).mapToObj(
				y -> IntStream.range(0, mat.length).mapToObj(
						x -> mat[x][y] ? trueStr : falseStr
				).collect(Collectors.joining())
		).collect(Collectors.joining("\n"));
	}
	
	public static int getColumnLength(boolean[][] mat) {
		int l = mat[0].length;
		for (int i=1; i<mat.length; i++) {
			if (mat[i].length != l) return -1;
		}
		return l;
	}
	
	public static Boolean[] box(boolean[] a) {
		Boolean[] b = new Boolean[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Boolean.valueOf(a[i]);
		}
		return b;
	}
	public static Character[] box(char[] a) {
		Character[] b = new Character[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Character.valueOf(a[i]);
		}
		return b;
	}
	public static Byte[] box(byte[] a) {
		Byte[] b = new Byte[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Byte.valueOf(a[i]);
		}
		return b;
	}
	public static Short[] box(short[] a) {
		Short[] b = new Short[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Short.valueOf(a[i]);
		}
		return b;
	}
	public static Integer[] box(int[] a) {
		Integer[] b = new Integer[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Integer.valueOf(a[i]);
		}
		return b;
	}
	public static Long[] box(long[] a) {
		Long[] b = new Long[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Long.valueOf(a[i]);
		}
		return b;
	}
	public static Float[] box(float[] a) {
		Float[] b = new Float[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Float.valueOf(a[i]);
		}
		return b;
	}
	public static Double[] box(double[] a) {
		Double[] b = new Double[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = Double.valueOf(a[i]);
		}
		return b;
	}
	
	public static boolean[] unbox(Boolean[] a) {
		boolean[] b = new boolean[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].booleanValue();
		}
		return b;
	}
	public static char[] unbox(Character[] a) {
		char[] b = new char[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].charValue();
		}
		return b;
	}
	public static byte[] unbox(Byte[] a) {
		byte[] b = new byte[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].byteValue();
		}
		return b;
	}
	public static short[] unbox(Short[] a) {
		short[] b = new short[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].shortValue();
		}
		return b;
	}
	public static int[] unbox(Integer[] a) {
		int[] b = new int[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].intValue();
		}
		return b;
	}
	public static long[] unbox(Long[] a) {
		long[] b = new long[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].longValue();
		}
		return b;
	}
	public static float[] unbox(Float[] a) {
		float[] b = new float[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].floatValue();
		}
		return b;
	}
	public static double[] unbox(Double[] a) {
		double[] b = new double[a.length];
		for (int i=0; i<a.length; i++) {
			b[i] = a[i].doubleValue();
		}
		return b;
	}
	
}
