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

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.ast.Annotation;
import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;
import lombok.ast.TypeReference;
import lombok.ast.VariableDefinition;

/**
 * Created by roman on 11/12/16.
 */
public class NullabilityDetector extends Detector implements Detector.JavaScanner {
    private static final String PARAMETER_ISSUE_ID = "MethodParameterNullability";
    private static final String PARAMETER_ISSUE_BRIEF_TEXT = "Annotate parameter with @Nullable/@NonNull annotation";
    private static final String PARAMETER_ISSUE_EXPLANATION = "All method params should be annotated with nullability check annotations";

    private static final String RETURN_VALUE_ISSUE_ID = "MethodReturnValueNullability";
    private static final String RETURN_VALUE_ISSUE_BRIEF_TEXT = "Annotate method with @Nullable/@NonNull annotation";
    private static final String RETURN_VALUE_ISSUE_EXPLANATION = "Methods should declare its return value nullability";

    private static final int ISSUE_PRIORITY = 6;

    static final Issue PARAMETER_NULLABILITY_ISSUE = Issue.create(
            PARAMETER_ISSUE_ID,
            PARAMETER_ISSUE_BRIEF_TEXT,
            PARAMETER_ISSUE_EXPLANATION,
            Category.CORRECTNESS,
            ISSUE_PRIORITY,
            Severity.WARNING,
            new Implementation(NullabilityDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    static final Issue RETURN_VALUE_NULLABILITY_ISSUE = Issue.create(
            PARAMETER_ISSUE_ID,
            PARAMETER_ISSUE_BRIEF_TEXT,
            PARAMETER_ISSUE_EXPLANATION,
            Category.CORRECTNESS,
            ISSUE_PRIORITY,
            Severity.WARNING,
            new Implementation(NullabilityDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    private static final Set<String> ANNOTATION_TYPES = set(
            "Nullable",
            "NonNull",
            "NotNull");


    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.singletonList(MethodDeclaration.class);
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elements) {
        return new HashSet<>(Lists.newArrayList(elements));
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new MethodDeclarationVisitor(context);
    }

    private static class MethodDeclarationVisitor extends ForwardingAstVisitor {

        private JavaContext context;

        public MethodDeclarationVisitor(@NonNull JavaContext context) {
            this.context = context;
        }

        @Override
        public boolean visitMethodDeclaration(@NonNull MethodDeclaration node) {
            checkReturnValue(node);

            StrictListAccessor<VariableDefinition, MethodDeclaration> variableDefinitions = node.astParameters();
            if (variableDefinitions.isEmpty()) {
                return super.visitMethodDeclaration(node);
            }

            checkParameters(variableDefinitions);

            return super.visitMethodDeclaration(node);
        }

        private void checkReturnValue(@NotNull MethodDeclaration node) {
            TypeReference returnType = node.astReturnTypeReference();
            if (returnType.isVoid() || returnType.isPrimitive()) {
                return;
            }

            Modifiers modifiers = node.astModifiers();
            boolean isAnnotated = isAnnotated(modifiers);
            if (!isAnnotated) {
                reportIssue(RETURN_VALUE_NULLABILITY_ISSUE, node);
            }
        }

        private void checkParameters(@NotNull StrictListAccessor<VariableDefinition, MethodDeclaration> variableDefinitions) {
            for (VariableDefinition variableDefinition : variableDefinitions) {
                if (!checkParameter(variableDefinition)) {
                    reportIssue(PARAMETER_NULLABILITY_ISSUE, variableDefinition);
                }
            }
        }

        private boolean checkParameter(@NotNull VariableDefinition variableDefinition) {
            TypeReference typeReference = variableDefinition.astTypeReference();
            boolean isPrimitive = typeReference.isPrimitive();

            if (isPrimitive) {
                return true;
            }

            Modifiers modifiers = variableDefinition.astModifiers();
            return isAnnotated(modifiers);
        }

        private boolean isAnnotated(@NotNull Modifiers modifiers) {
            boolean isAnnotated = false;

            for (Node modifier : modifiers.getChildren()) {
                if (modifier instanceof Annotation) {
                    isAnnotated = checkAnnotation((Annotation) modifier);

                    if (isAnnotated) {
                        break;
                    }
                }
            }
            return isAnnotated;
        }

        private boolean checkAnnotation(@NotNull Annotation modifier) {
            TypeReference typeReference = modifier.astAnnotationTypeReference();
            return ANNOTATION_TYPES.contains(typeReference.getTypeName());
        }

        private void reportIssue(@NotNull Issue issue, @NotNull Node node) {
            context.report(issue, node, context.getLocation(node), issue.getBriefDescription(TextFormat.TEXT));
        }
    }
}
