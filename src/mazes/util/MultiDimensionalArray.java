package mazes.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public final class MultiDimensionalArray<T> implements Serializable {
	
	public static final long serialVersionUID = -5377388193057183350L;
	
	private final int[] sideLengths;
	public final int dimensionCount;
	
	private final int[] cumulativeRowLengths;
	public final int totalLength;
	
	private final T[] data;
	
	@SuppressWarnings("unchecked")
	public MultiDimensionalArray(int[] sideLengths) {
		if (sideLengths == null) throw new NullPointerException();
		if (sideLengths.length == 0) throw new IllegalArgumentException();
		
		this.sideLengths = sideLengths;
		this.dimensionCount = sideLengths.length;
		
		this.cumulativeRowLengths = new int[dimensionCount];
		int cumulativeRowLength = 1;
		for (int i=0; i<dimensionCount; i++) {
			cumulativeRowLengths[i] = cumulativeRowLength;
			cumulativeRowLength *= sideLengths[i];
		}
		this.totalLength = cumulativeRowLength;
		
		this.data = (T[]) new Object[totalLength];
	}
	
	public int getSideLength(int index) {
		return sideLengths[index];
	}
	public int[] getSideLengths() {
		return Arrays.copyOf(sideLengths, dimensionCount);
	}
	
	public boolean isValidIndex(int[] indices) {
		try {
			indicesToIndex(indices);
			return true;
		}
		catch (IndexOutOfBoundsException e) {
			return false;
		}
	}
	public T get(int[] indices) {
		return data[indicesToIndex(indices)];
	}
	public void set(int[] indices, T obj) {
		data[indicesToIndex(indices) ] = obj;
	}
	public void fill(T initialValue) {
		for (int i=0; i<totalLength; i++) {
			data[i] = initialValue;
		}
	}
	public void fill(Supplier<T> initialValue) {
		if (initialValue == null) throw new NullPointerException();
		for (int i=0; i<totalLength; i++) {
			data[i] = initialValue.get();
		}
	}
	
	private static boolean isInterior(int[] index, int[] sideLengths) {
		int dimensionCount = sideLengths.length;
		if (index == null) throw new NullPointerException();
		if (index.length != dimensionCount) throw new IllegalArgumentException();
		for (int d=0; d<dimensionCount; d++) {
			if (index[d] < 0 || index[d] >= sideLengths[d]) {
				return false;
			}
		}
		return true;
	}
	private static boolean isInteriorOrBorder(int[] index, int[] sideLengths) {
		int dimensionCount = sideLengths.length;
		if (index == null) throw new NullPointerException();
		if (index.length != dimensionCount) throw new IllegalArgumentException();
		int violations = 0;
		for (int d=0; d<dimensionCount; d++) {
			if (index[d] < 0 || index[d] >= sideLengths[d]) {
				violations += 1;
			}
		}
		return violations < 2;
	}
	private static boolean isInsideBorder(int[] index, int[] sideLengths) {
		int dimensionCount = sideLengths.length;
		if (index == null) throw new NullPointerException();
		if (index.length != dimensionCount) throw new IllegalArgumentException();
		int violations = 0;
		for (int d=0; d<dimensionCount; d++) {
			if (index[d] == 0 || index[d] == sideLengths[d] - 1) {
				violations += 1;
			}
			else if (index[d] < 0 || index[d] >= sideLengths[d]) {
				return false;
			}
		}
		return violations >= 1 && violations <= dimensionCount;
	}
	public boolean isInterior(int[] index) {
		return MultiDimensionalArray.isInterior(index, sideLengths);
	}
	public boolean isInteriorOrBorder(int[] index) {
		return MultiDimensionalArray.isInteriorOrBorder(index, sideLengths);
	}
	public boolean isInsideBorder(int[] index) {
		return MultiDimensionalArray.isInsideBorder(index, sideLengths);
	}
	public Direction getDirectionIntoGrid(int[] index) {
		if (index == null) throw new NullPointerException();
		if (index.length != dimensionCount) throw new IllegalArgumentException();
		for (int d=0; d<dimensionCount; d++) {
			if (index[d] < 0) {
				return new Direction(d, true);
			}
			if (index[d] >= sideLengths[d]) {
				return new Direction(d, false);
			}
		}
		throw new IllegalArgumentException();
	}
	public Direction getDirectionOutOfGrid(int[] index) {
		if (index == null) throw new NullPointerException();
		if (index.length != dimensionCount) throw new IllegalArgumentException();
		for (int d=0; d<dimensionCount; d++) {
			if (index[d] == 0) {
				return new Direction(d, false);
			}
			if (index[d] == sideLengths[d] - 1) {
				return new Direction(d, true);
			}
		}
		throw new IllegalArgumentException();
	}
	
	public static List<int[]> getInteriorIndexList(int[] sideLengths) {
		if (sideLengths == null) throw new NullPointerException();
		int dimensionCount = sideLengths.length;
		List<int[]> indexList = new ArrayList<int[]>();
		int[] currentIndex = new int[dimensionCount];
		while (true) {
			indexList.add(Arrays.copyOf(currentIndex, currentIndex.length));
			for (int i=0; i<dimensionCount; i++) {
				if (currentIndex[i] == sideLengths[i] - 1) {
					currentIndex[i] = 0;
					if (i == dimensionCount - 1) {
						return indexList;
					}
				}
				else {
					currentIndex[i] += 1;
					break;
				}
			}
		}
	}
	
	public static List<int[]> getBorderIndexList(int[] sideLengths) {
		if (sideLengths == null) throw new NullPointerException();
		int dimensionCount = sideLengths.length;
		List<int[]> indexList = new ArrayList<int[]>();
		int[] currentIndex = new int[dimensionCount];
		while (true) {
			if (MultiDimensionalArray.isInsideBorder(currentIndex, sideLengths)) {
				indexList.add(Arrays.copyOf(currentIndex, currentIndex.length));
			}
			for (int i=0; i<dimensionCount; i++) {
				if (currentIndex[i] == sideLengths[i] - 1) {
					currentIndex[i] = 0;
					if (i == dimensionCount - 1) {
						return indexList;
					}
				}
				else {
					currentIndex[i] += 1;
					break;
				}
			}
		}
	}
	
	private int indicesToIndex(int[] indices) {
		if (indices == null) throw new NullPointerException();
		if (indices.length != dimensionCount) throw new IllegalArgumentException();
		int index = 0;
		for (int i=0; i<dimensionCount; i++) {
			if (indices[i] < 0 || indices[i] >= sideLengths[i]) {
				throw new IndexOutOfBoundsException();
			}
			index += indices[i] * cumulativeRowLengths[i];
		}
		return index;
	}
	
	@Override public String toString() {
		return getSubstring(new int[0]);
	}
	private String getSubstring(int[] indices) {
		if (indices.length == dimensionCount) {
			return get(indices).toString();
		}
		else {
			StringBuilder sb = new StringBuilder("[");
			int dimension = indices.length;
			for (int i=0; i<getSideLength(dimension); i++) {
				int[] newIndices = Arrays.copyOf(indices, dimension+1);
				newIndices[dimension] = i;
				if (i != 0) sb.append(", ");
				sb.append(getSubstring(newIndices));
			}
			sb.append("]");
			return sb.toString();
		}
	}
	
}
