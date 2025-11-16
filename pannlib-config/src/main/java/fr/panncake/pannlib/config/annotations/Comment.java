package fr.panncake.pannlib.config.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(Comments.class)
public @interface Comment {
    String[] value();
}