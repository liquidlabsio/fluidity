/*
 *  Copyright (c) 2020. Liquidlabs Ltd <info@liquidlabs.com>
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.fluidity.search.field;

import io.fluidity.search.Search;

public class TagMatcher {
    public static final String PREFIX1 = "tags.contains(";
    public static final String PREFIX2 = "tags.equals(";
    private final String expressionPart;
    private boolean containsMatch;

    public TagMatcher(String expression) {
        String[] split = expression.split("\\|");

        String passedFilenameExpression = split.length > Search.EXPRESSION_PARTS.bucket.ordinal() ? split[Search.EXPRESSION_PARTS.bucket.ordinal()].trim() : "";
        if (passedFilenameExpression.startsWith(PREFIX1)) {
            int startsFrom = expression.indexOf(PREFIX1);
            int endsAt = expression.indexOf(")", startsFrom);
            this.expressionPart = expression.substring(startsFrom + PREFIX1.length(), endsAt);
            containsMatch = true;
        } else if (passedFilenameExpression.startsWith(PREFIX2)) {
            int startsFrom = expression.indexOf(PREFIX2);
            int endsAt = expression.indexOf(")", startsFrom);
            this.expressionPart = expression.substring(startsFrom + PREFIX2.length(), endsAt);
            containsMatch = false;
        } else if (passedFilenameExpression.equals("*")) {
            this.expressionPart = "*";
        } else {
            this.expressionPart = "*";
        }

    }

    public boolean matches(String tags) {
        return expressionPart.equals("*") || containsMatch && tags.contains(expressionPart) || !containsMatch && tags.equals(expressionPart);
    }
}
