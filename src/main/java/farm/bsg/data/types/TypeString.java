package farm.bsg.data.types;

import farm.bsg.data.Type;
import farm.bsg.data.contracts.ProjectionProvider;

public class TypeString extends Type {

    private boolean emptyIsNull = false;
    private boolean alwaysTrim  = false;

    public TypeString(String name) {
        super(name);
    }

    public TypeString emptyStringSameAsNull() {
        this.emptyIsNull = true;
        return this;
    }

    public TypeString alwaysTrim() {
        this.alwaysTrim = true;
        return this;
    }

    @Override
    public String type() {
        return "string";
    }

    @Override
    public String normalize(String valueRaw) {
        String value = valueRaw;
        if (value == null) {
            return value;
        }
        if (alwaysTrim) {
            value = value.trim();
        }
        if (emptyIsNull && "".equals(value)) {
            return null;
        }
        return value;
    }

    @Override
    public boolean validate(String value) {
        return true;
    }

    @Override
    public String defaultValue() {
        return null;
    }

    public static String project(ProjectionProvider provider, String key) {
        return provider.first(key);
    }
}