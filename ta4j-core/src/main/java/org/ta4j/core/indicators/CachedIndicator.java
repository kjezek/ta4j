/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2019 Ta4j Organization & respective
 * authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ta4j.core.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Cached {@link Indicator indicator}.
 *
 * Caches the constructor of the indicator. Avoid to calculate the same index of
 * the indicator twice.
 */
public abstract class CachedIndicator<T> extends AbstractIndicator<T> {

    private static final long serialVersionUID = 7505855220893125595L;

    /**
     * List of cached results
     */
//    private final List<T> results = new ArrayList<T>();
    private final Map<Integer, T> results = new HashMap<>();

    /**
     * Should always be the index of the last result in the results list. I.E. the
     * last calculated result.
     */
    protected int highestResultIndex = -1;

    private int lastDeleted = 0;

    /**
     * Constructor.
     *
     * @param series the related bar series
     */
    public CachedIndicator(BarSeries series) {
        super(series);
//        int size = Math.min(1000000, series.getMaximumBarCount());
//        results.addAll(Collections.nCopies(size, null));
    }

    /**
     * Constructor.
     *
     * @param indicator a related indicator (with a bar series)
     */
    public CachedIndicator(Indicator<?> indicator) {
        this(indicator.getBarSeries());
    }

    @Override
    public T getValue(int index) {

        final BarSeries series = getBarSeries();
        final int removedBarsCount = series.getRemovedBarsCount();
        final int maximumResultCount = series.getMaximumBarCount();

        T t;
        if (index == series.getEndIndex()) {
            // last index is always calculated because it can be changed e.g. by calling bar#addPrice
            t = calculate(index);
        } else {

            // when the index is from non-existing range,
            // first element is used - this is from orig ta4j, it is obscure trick to prevent recursion
            // https://github.com/mdeverdelhan/ta4j/issues/120
            final int efIndex;
            if (index < removedBarsCount) {
                efIndex = 0;
            } else {
                efIndex = index;
            }

            // cannot use computeIfAbsent as it throws ConcurrentModificationEx
            // for recursive calls.
            t = results.get(efIndex);
            if (t == null) {
                t = calculate(efIndex);
                results.put(efIndex, t);
                highestResultIndex = Math.max(Math.max(removedBarsCount, index), highestResultIndex);
            }
        }

        // remove unused indexes only once in a while, not for every getValue
        if (results.size() * 10 > maximumResultCount) {
            IntStream.range(lastDeleted, removedBarsCount).forEach(results::remove);
            lastDeleted = removedBarsCount;
        }

        return t;
    }

    /**
     * @param index the bar index
     * @return the value of the indicator
     */
    protected abstract T calculate(int index);

}
