/*******************************************************************************
 * Copyright (c) 2014-2016 Radon Rosborough. All rights reserved.
 *******************************************************************************/
package util;

import java.math.BigInteger;
import java.util.stream.IntStream;

public final class MathUtil {
	
	private MathUtil() {}
	
	public static void testFractions() {
		for (int n : IntStream.rangeClosed(-10, 10).toArray()) {
			System.out.printf("%s --> %s%n", toEighthsDecimal(n), toEighthsFraction(n));
		}
	}
	
	// Yeah, it's unreadable. Sorry. It was fun to write, though.
	public static String toEighthsFraction(int n) {
		String whole = (n / 8 == 0 ^ n == 0) ? (n < 0 ? "-" : "") : String.valueOf(n / 8);
		String sep = n / 8 == 0 || n % 8 == 0 ? "" : " ";
		String frac;
		switch (Math.abs(n) % 8) {
		case 0: frac = ""; break;
		case 1: frac = "1/8"; break;
		case 2: frac = "1/4"; break;
		case 3: frac = "3/8"; break;
		case 4: frac = "1/2"; break;
		case 5: frac = "5/8"; break;
		case 6: frac = "3/4"; break;
		case 7: frac = "7/8"; break;
		default: throw new IllegalArgumentException();
		}
		return whole + sep + frac;
	}
	public static String toEighthsDecimal(int n) {
		String whole = (n < 0 && n / 8 == 0 ? "-" : "") + String.valueOf(n / 8);
		String frac;
		switch (Math.abs(n) % 8) {
		case 0: frac = ""; break;
		case 1: frac = ".125"; break;
		case 2: frac = ".25"; break;
		case 3: frac = ".375"; break;
		case 4: frac = ".5"; break;
		case 5: frac = ".625"; break;
		case 6: frac = ".75"; break;
		case 7: frac = ".875"; break;
		default: throw new IllegalArgumentException();
		}
		return whole + frac;
	}
	
	public static int mod(int a, int q) {
		int r = a % q;
		return r < 0 ? r + q : r;
	}
	public static long mod(long a, long q) {
		long r = a % q;
		return r < 0 ? r + q : r;
	}
	public static float mod(float a, float q) {
		float r = a % q;
		return r < 0 ? r + q : r;
	}
	public static double mod(double a, double q) {
		double r = a % q;
		return r < 0 ? r + q : r;
	}
	
	public static int downcast(long l) {
		return (int)l;
	}
	public static long upcast(int i) {
		return i & 0xFFFF_FFFFL;
	}
	
	public static String toHexString(byte b) {
		return String.format("%02X", b);
	}
	public static String toHexString(short s) {
		return String.format("%04X", s);
	}
	public static String toHexString(int i) {
		return String.format("%08X", i);
	}
	public static String toHexString(long l) {
		return String.format("%016X", l);
	}
	public static String toHexString(float f) {
		return toHexString(Float.floatToRawIntBits(f));
	}
	public static String toHexString(double d) {
		return toHexString(Double.doubleToRawLongBits(d));
	}
	
	public static byte parseByte(String s, int radix) {
		BigInteger n = new BigInteger(s, radix);
		return n.byteValue();
	}
	public static byte parseByte(String s) {
		return parseByte(s, 10);
	}
	public static short parseShort(String s, int radix) {
		BigInteger n = new BigInteger(s, radix);
		return n.shortValue();
	}
	public static short parseShort(String s) {
		return parseShort(s, 10);
	}
	public static int parseInt(String s, int radix) {
		BigInteger n = new BigInteger(s, radix);
		return n.intValue();
	}
	public static int parseInt(String s) {
		return parseInt(s, 10);
	}
	public static long parseLong(String s, int radix) {
		BigInteger n = new BigInteger(s, radix);
		return n.longValue();
	}
	public static long parseLong(String s) {
		return parseLong(s, 10);
	}
	
}
