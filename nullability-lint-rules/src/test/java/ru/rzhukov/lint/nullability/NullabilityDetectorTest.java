/*
 * Copyright 2016 Roman Zhukov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.rzhukov.lint.nullability;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by roman on 11/12/16.
 */
public class NullabilityDetectorTest extends LintDetectorTest {

    public static final String NO_WARNINGS = "No warnings.";

    public void testObjectParameter() throws Exception {
        assertNotSame(NO_WARNINGS, lintFiles("TestObjectMethodParameter.java"));
    }


    public void testPrimitiveParameter() throws Exception {
        assertEquals(NO_WARNINGS, lintFiles("TestPrimitiveMethodParameter.java"));
    }

    public void testNoParameters() throws Exception {
        assertEquals(NO_WARNINGS, lintFiles("TestMethodWithoutParameter.java"));
    }

    public void testReturnValue() throws Exception {
        String lintResult = lintFiles("TestMethodReturnValue.java");
        assertNotSame(lintResult, NO_WARNINGS, lintResult);
    }

    public void testReturnVoid() throws Exception {
        String lintResult = lintFiles("TestMethodReturnVoid.java");
        assertEquals(lintResult, NO_WARNINGS, lintResult);
    }

    @Override
    protected InputStream getTestResource(String relativePath, boolean expectExists) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(relativePath);
        if (!expectExists && stream == null) {
            return null;
        }
        return stream;
    }

    @Override
    protected Detector getDetector() {
        return new NullabilityDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Arrays.asList(
                NullabilityDetector.RETURN_VALUE_NULLABILITY_ISSUE,
                NullabilityDetector.PARAMETER_NULLABILITY_ISSUE
        );
    }
}