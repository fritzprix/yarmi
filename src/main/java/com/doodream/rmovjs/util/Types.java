package com.doodream.rmovjs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Types {
    private static Pattern INTERNAL_TYPE_SELECTOR_PATTERN = Pattern.compile("([^\\<\\>]+)\\<([\\s\\S]+)\\>");
    private static final Logger Log = LoggerFactory.getLogger(Types.class);

    public static Type[] unwrapType(String typeName) throws ClassNotFoundException, IllegalArgumentException {
        Matcher matcher = INTERNAL_TYPE_SELECTOR_PATTERN.matcher(typeName);
        if (matcher.matches()) {
            // try unwrap recursive
            String unwrapped = matcher.group(2);
            String[] split = splitTypeParameter(unwrapped);
            List<Type> types = new ArrayList<>();
            for (String s : split) {
                try {
                    final Type[] parameters = unwrapType(s);
                    matcher = INTERNAL_TYPE_SELECTOR_PATTERN.matcher(s);
                    if(matcher.matches()) {
                        final Type rawClass = Class.forName(matcher.group(1));
                        types.add(new ParameterizedType() {
                            @Override
                            public Type[] getActualTypeArguments() {
                                return parameters;
                            }

                            @Override
                            public Type getRawType() {
                                return rawClass;
                            }

                            @Override
                            public Type getOwnerType() {
                                return null;
                            }
                        });
                    }
                } catch (IllegalArgumentException e) {
                    types.add(Class.forName(s));
                }
            }
            return types.toArray(new Type[0]);
        } else {
            throw new IllegalArgumentException("Nothing to unwrap");
        }
    }

    /**
     * split type parameter at outermost scope
     * for example -> Map<Map<String,String>, Map<String,String>>, String
     *                                             split here -> |
     * @param original typeName from {@link Type}
     * @return if there is split return type names as string array, otherwise array contains original
     */
    private static String[] splitTypeParameter(String original) {
        int pos;
        String chunk = original.trim();
        StringBuilder lineSeparatedParameterBuilder = new StringBuilder();
        while (((pos = findNextTypeParameterIndex(chunk)) > 0)) {
            lineSeparatedParameterBuilder.append(chunk, 0, pos);
            lineSeparatedParameterBuilder.append("\n");
            chunk = chunk.substring(pos + 1);
        }
        lineSeparatedParameterBuilder.append(chunk.trim());
        lineSeparatedParameterBuilder.append("\n");
        return lineSeparatedParameterBuilder.toString().split("\n");
    }

    private static int findNextTypeParameterIndex(String original) {
        int pos = 0,i;
        final int len = original.length();
        for (i = 0; i < len; i++) {
            switch (original.charAt(i)) {
                case '<':
                    pos++;
                    break;
                case '>':
                    pos--;
                    break;
                case ',':
                    if(pos == 0) {
                        return i;
                    }
                    break;
                default:
                    break;
            }
        }
        return 0;
    }
}
