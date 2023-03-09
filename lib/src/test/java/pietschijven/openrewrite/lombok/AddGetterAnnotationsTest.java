package pietschijven.openrewrite.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddGetterAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddGetterAnnotations());
    }

    @Test
    void addsGetterAnnotation() {

        rewriteRun(
                java(
                    """
                            package com.yourorg;
                                            
                            public class Customer {
                                
                                private String name;
                                
                                private int date;
                                
                                public String getName() {
                                    return this.name;
                                }
                            }
                            """,
                    """
                            package com.yourorg;
                                                    
                            import lombok.Getter;
                                                    
                            public class Customer {
                                
                                @Getter
                                private String name;
                                
                                private int date;
                            }
                            """
                )
        );
    }

    @Test
    void doesNotRemoveUnknownGetter() {
        rewriteRun(
                java(
                        """
                                package com.yourorg;
                                                
                                public class Customer {
                                
                                    private String name;
                                    
                                    public String getFoo() {
                                        return "foo";
                                    }
                                }
                                """
                )
        );
    }

}