package util;

public final class Util {
	
	private Util() {}
	
	public static <T> T def(T obj, T def) {
		return obj != null ? obj : def;
	}
	@SafeVarargs public static <T> T firstNonNull(T... objs) {
		for (T obj : objs) {
			if (obj != null) return obj;
		}
		throw new NullPointerException();
	}
	
}
