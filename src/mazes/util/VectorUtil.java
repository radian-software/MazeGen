package mazes.util;

import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public final class VectorUtil {
	
	private VectorUtil() {}
	
	public static int[] sumVectors(int[]... vectors) {
		if (vectors.length < 2) throw new IllegalArgumentException();
		int[] sum = Arrays.copyOf(vectors[0], vectors[0].length);
		for (int i=1; i<vectors.length; i++) {
			for (int j=0; j<sum.length; j++) {
				sum[j] += vectors[i][j];
			}
		}
		return sum;
	}
	public static int[] subtractVectors(int[] vector1, int[] vector2) {
		int[] diff = Arrays.copyOf(vector1, vector1.length);
		for (int i=0; i<diff.length; i++) {
			diff[i] -= vector2[i];
		}
		return diff;
	}
	public static int[] scalarMultiple(int[] vector, int scalar) {
		int[] newVector = Arrays.copyOf(vector, vector.length);
		for (int i=0; i<vector.length; i++) {
			newVector[i] *= scalar;
		}
		return newVector;
	}
	
	public static void forEach(int[] vector, IntUnaryOperator operator) {
		for (int i=0; i<vector.length; i++) {
			vector[i] = operator.applyAsInt(vector[i]);
		}
	}
	public static int countFilter(int[] vector, IntPredicate predicate) {
		int count = 0;
		for (int i=0; i<vector.length; i++) {
			if (predicate.test(vector[i])) {
				count += 1;
			}
		}
		return count;
	}
	
}
