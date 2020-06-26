/*
 *
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software  distributed under the License is distributed on an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *   See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package io.fluidity.search.field.extractor;

import io.fluidity.util.StringUtil;
import org.graalvm.collections.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Support "CPU: XXX" - using regexp pattern
 */
public class KvPairExtractor implements Extractor {

    private final Pattern pattern;

    public KvPairExtractor(String expressionPart) {
        String patternString = ".*(" + expressionPart + ")(\\S+).*";
        this.pattern = Pattern.compile(patternString);
    }

    @Override
    public Pair<String, Long> getKeyAndValue(String sourceName, String nextLine) {
        Matcher matcher = pattern.matcher(nextLine);
        if (matcher.matches()) {
            if (StringUtil.isNumber(matcher.group(2))) {
                return Pair.create(matcher.group(1).trim(), Long.parseLong(matcher.group(2)));
            } else {
                return Pair.create(matcher.group(2).trim(), 1l);
            }
        } else {
            return null;
        }
    }
}
