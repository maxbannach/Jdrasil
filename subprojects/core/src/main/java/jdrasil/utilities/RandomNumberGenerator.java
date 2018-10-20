/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.utilities;

import java.util.Random;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Static overlay of Javas random number generator, as we globally need a single random sequence, that can
 * be seeded with a single seed.
 *
 * This should be the only source of randomness used by classes and methods of Jdrasil.
 *
 * @author Max Bannach
 */
public class RandomNumberGenerator {

    /** The random source of Jdrasil. */
    private static Random dice;

    /* Static constructor that just will load the Random object. */
    static {
        dice = new Random();
    }

    /**
     * Seed the random number generator of Jdrasil
     * @param seed to be used
     */
    public static void seed(long seed) {
        dice.setSeed(seed);
    }

    /**
     * @see Random#nextInt()
     * @return random integer
     */
    public static int nextInt() {
        return dice.nextInt();
    }

    /**
     * @see Random#nextInt(int bound)
     * @param bound - the generated random number will be smaller
     * @return random integer less then the given bound
     */
    public static int nextInt(int bound) {
        return dice.nextInt(bound);
    }

    /**
     * @see Random#nextDouble()
     * @return random double
     */
    public static double nextDouble() {
        return dice.nextDouble();
    }

    /**
     * @see Random#nextLong()
     * @return random long
     */
    public static long nextLong() {
        return dice.nextLong();
    }

    /**
     * @see Random#nextBoolean() ()
     * @return random boolean
     */
    public static boolean nextBoolean() {
        return dice.nextBoolean();
    }

    /**
     * @see Random#nextFloat()
     * @return random float
     */
    public static float nextFloat() {
        return dice.nextFloat();
    }

    /**
     * @see Random#nextGaussian()
     * @return random double
     */
    public static double nextGaussian() {
        return dice.nextGaussian();
    }

    /**
     * @see Random#nextBytes(byte[])
     */
    public static void nextBytes(byte[] bytes) {
        dice.nextBytes(bytes);
    }

    /**
     * @see RandomNumberGenerator#ints()
     * @return random IntStream
     */
    public static IntStream ints() {
        return dice.ints();
    }

    /**
     * @see Random#ints(int, int)
     * @return random IntStream
     */
    public static IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        return dice.ints(randomNumberOrigin, randomNumberBound);
    }

    /**
     * @see Random#ints(long)
     * @return random IntStream
     */
    public static IntStream ints(long streamSize) {
        return dice.ints(streamSize);
    }

    /**
     * @see Random#ints(long, int, int)
     * @return random IntStream
     */
    public static IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        return dice.ints(streamSize, randomNumberOrigin, randomNumberBound);
    }

    /**
     * @see RandomNumberGenerator#doubles()
     * @return random DoubleStream
     */
    public static DoubleStream doubles() {
        return dice.doubles();
    }

    /**
     * @see RandomNumberGenerator#doubles(double, double)
     * @return random DoubleStream
     */
    public static DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        return dice.doubles(randomNumberOrigin,randomNumberBound);
    }

    /**
     * @see RandomNumberGenerator#doubles(long)
     * @return random DoubleStream
     */
    public static DoubleStream doubles(long streamSize) {
        return dice.doubles(streamSize);
    }

    /**
     * @see RandomNumberGenerator#doubles(long, double, double)
     * @return random DoubleStream
     */
    public static DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
        return dice.doubles(streamSize, randomNumberOrigin, randomNumberBound);
    }

    /**
     * @see RandomNumberGenerator#longs()
     * @return random LongStream
     */
    public static LongStream longs() {
        return dice.longs();
    }

    /**
     * @see RandomNumberGenerator#longs(long)
     * @return random LongStream
     */
    public static LongStream longs(long streamSize) {
        return dice.longs(streamSize);
    }

    /**
     * @see RandomNumberGenerator#longs(long, long)
     * @return random LongStream
     */
    public static LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        return dice.longs(randomNumberOrigin, randomNumberBound);
    }

    /**
     * @see RandomNumberGenerator#longs(long, long, long)
     * @return random LongStream
     */
    public static LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        return dice.longs(streamSize, randomNumberOrigin, randomNumberBound);
    }

    /**
     * Get the underlying random object.
     * @return
     */
    public static Random getDice() {
        return dice;
    }
}
