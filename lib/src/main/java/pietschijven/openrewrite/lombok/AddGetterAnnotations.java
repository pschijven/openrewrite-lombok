package pietschijven.openrewrite.lombok;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class AddGetterAnnotations extends Recipe {

    @Override
    public String getDisplayName() {
        return "Lombok - Add Getter-annotation to field";
    }

    @Override
    public String getDescription() {
        return "Adds the Lombok @Getter annotation to a field, if there is a corresponding getter method.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            private final JavaTemplate addAnnotation =
                    JavaTemplate.builder(this::getCursor, "@Getter")
                    .imports("lombok.Getter")
                    .javaParser(() -> JavaParser.fromJavaVersion()
                            .dependsOn("package lombok;"
                                    + "import java.lang.annotation.ElementType;\n" +
                                    "import java.lang.annotation.Retention;\n" +
                                    "import java.lang.annotation.RetentionPolicy;\n" +
                                    "import java.lang.annotation.Target;" +
                                    "@Target({ElementType.FIELD, ElementType.TYPE})\n" +
                                    "@Retention(RetentionPolicy.SOURCE)\n" +
                                    "public @interface Getter {" +
                                    "}")
                            .build())
                    .build();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, executionContext);

                // Find all getter methods for fields that have the @Getter annotation
                var methodsToRemove =  c.getBody().getStatements().stream()
                        .filter(it -> it instanceof J.VariableDeclarations)
                        .map(it -> (J.VariableDeclarations) it)
                        .filter(this::hasGetterAnnotation)
                        .flatMap(it -> it.getVariables().stream())
                        .flatMap(it -> findGetterMethods(classDecl, it).stream())
                        .collect(Collectors.toSet());

                // Remove all these getter methods from the body of the class declaration
                var statements = c.getBody().getStatements()
                        .stream()
                        .filter(statement -> {
                            if (statement instanceof J.MethodDeclaration) {
                                J.MethodDeclaration method = (J.MethodDeclaration) statement;
                                return !methodsToRemove.contains(method);
                            }
                            return true;
                        }).collect(Collectors.toList());

                c = c.withBody(c.getBody().withStatements(statements));
                return c;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations multiVariable, ExecutionContext executionContext) {

                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, executionContext);

                // Return if variable declaration is not a field, already has a Getter annotation
                // or if there is a multi-variable Declaration
                if (!isField(getCursor())
                        || hasGetterAnnotation(v)
                        || v.getVariables().size() != 1) {

                    return v;
                }

                // Check if Getter Method exists.
                if (hasGetterMethod(v)) {

                    // Add Annotation to the variable Declaration
                    v = v.withTemplate(addAnnotation,
                            v.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                    maybeAddImport("lombok.Getter");
                }

                return v;
            }

            /**
             *
             */
            private boolean hasGetterMethod(J.VariableDeclarations v) {
                J.ClassDeclaration enclosingClass = getCursor()
                        .firstEnclosingOrThrow(J.ClassDeclaration.class);

                return v.getVariables()
                        .stream()
                        .noneMatch(variable -> findGetterMethods(enclosingClass, variable).isEmpty());
            }

            private Set<J.MethodDeclaration> findGetterMethods(J.ClassDeclaration enclosingClass,
                                                               J.VariableDeclarations.NamedVariable variable) {
                return FindMethods.findDeclaration(enclosingClass, getterMethodPattern(variable));
            }

            private String getterMethodPattern(J.VariableDeclarations.NamedVariable it) {
                String prefix = TypeUtils.isOfClassType(it.getType(),"boolean") ? "is" : "get";
                return "* " + prefix + StringUtils.capitalize(it.getSimpleName()) + "()";
            }

            private boolean hasGetterAnnotation(J.VariableDeclarations v) {
                return v.getAllAnnotations()
                        .stream()
                        .anyMatch(it -> TypeUtils.isOfClassType(it.getType(), "lombok.Getter"));
            }

            private boolean isField(Cursor cursor) {
                return cursor
                        .dropParentUntil(parent -> parent instanceof J.ClassDeclaration
                                || parent instanceof J.MethodDeclaration)
                        .getValue() instanceof J.ClassDeclaration;
            }
        };
    }

}
