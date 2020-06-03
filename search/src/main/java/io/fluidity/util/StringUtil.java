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

package io.fluidity.util;

public class StringUtil {
    public static boolean containsNumber(String str) {
        char[] chars = str.toCharArray();
        for (char aChar : chars) {
            if (aChar >= '0' && aChar <= '9') return true;
        }
        return false;
    }

    public static boolean isNumber(String str) {
        char[] chars = str.toCharArray();
        int count = 0;
        for (char aChar : chars) {
            if (aChar >= '0' && aChar <= '9') count++;
        }
        return count == chars.length;
    }

}
