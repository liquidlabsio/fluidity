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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagMatcherTest {

    @Test
    void contains() {
        TagMatcher matcher = new TagMatcher("tags.contains(123) |*|*|*|*");
        assertTrue(matcher.matches("123"));
    }

    @Test
    void notContains() {
        TagMatcher matcher = new TagMatcher("tags.contains(123) |*|*|*|*");
        assertFalse(matcher.matches("128"));
    }

    @Test
    void equals() {
        TagMatcher matcher = new TagMatcher("tags.equals(123) |*|*|*|*");
        assertTrue(matcher.matches("123"));
    }

    @Test
    void notEquals() {
        TagMatcher matcher = new TagMatcher("tags.equals(123) |*|*|*|*");
        assertFalse(matcher.matches("1235"));
    }
}