package pietschijven.openrewrite.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.openrewrite.java.Assertions.java;

class AddGetterAnnotationsAcceptanceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        String yaml = """
                type: specs.openrewrite.org/v1beta/recipe
                name: pietschijven.openrewrite.lombok.IntroduceGetter
                recipeList:
                  - org.openrewrite.java.cleanup.MultipleVariableDeclarations
                  - pietschijven.openrewrite.lombok.AddGetterAnnotations
                """;

        spec.recipe(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "pietschijven.openrewrite.lombok.IntroduceGetter");
    }

    @Test
    void testRefactorToAnnotation() {

        rewriteRun(
                java(
                        """
                                package com.yourorg;
                                                
                                public class Customer {
                                    
                                    private String name;
                                    private int a, b=2;
                                    
                                    private String noGetter;
                                    
                                    public String getName() {
                                        return this.name;
                                    }
                                    
                                    public String getA() {
                                        return this.a;
                                    }
                                    
                                    public String getB() {
                                        return this.b;
                                    }
                                    
                                    public String getSomeData() {
                                        return "some data";
                                    }
                                }
                                """,
                        """
                                package com.yourorg;
                                
                                import lombok.Getter;
                                                        
                                public class Customer {
                                    
                                    @Getter
                                    private String name;
                                    @Getter
                                    private int a;
                                    @Getter
                                    private int b = 2;
                                    
                                    private String noGetter;
                                    
                                    public String getSomeData() {
                                        return "some data";
                                    }
                                }
                                """
                )
        );
    }
}