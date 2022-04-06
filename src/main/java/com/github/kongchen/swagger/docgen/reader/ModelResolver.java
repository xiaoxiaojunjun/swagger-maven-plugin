//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.github.kongchen.swagger.docgen.reader;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.IntSequenceGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.None;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.PropertyGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Iterables;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.AbstractModelConverter;
import io.swagger.jackson.TypeNameResolver;
import io.swagger.jackson.TypeNameResolver.Options;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Xml;
import io.swagger.models.properties.AbstractNumericProperty;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.models.properties.PropertyBuilder.PropertyId;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UUIDProperty;
import io.swagger.models.refs.RefFormat;
import io.swagger.util.AllowableValues;
import io.swagger.util.AllowableValuesUtils;
import io.swagger.util.BaseReaderUtils;
import io.swagger.util.PrimitiveType;
import io.swagger.util.ReflectionUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelResolver extends AbstractModelConverter implements ModelConverter {
    Logger LOGGER = LoggerFactory.getLogger(ModelResolver.class);

    public ModelResolver(ObjectMapper mapper) {
        super(mapper);
    }

    public ModelResolver(ObjectMapper mapper, TypeNameResolver typeNameResolver) {
        super(mapper, typeNameResolver);
    }

    public ObjectMapper objectMapper() {
        return this._mapper;
    }

    protected boolean shouldIgnoreClass(Type type) {
        if (type instanceof Class) {
            Class<?> cls = (Class)type;
            if (cls.getName().equals("javax.ws.rs.Response")) {
                return true;
            }
        } else if (type instanceof ResolvedType) {
            ResolvedType rt = (ResolvedType)type;
            this.LOGGER.debug("Can't check class {}, {}", type, rt.getRawClass().getName());
            if (rt.getRawClass().equals(Class.class)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Property resolveProperty(Type type, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> next) {
        return this.shouldIgnoreClass(type) ? null : this.resolveProperty(this._mapper.constructType(type), context, annotations, next);
    }

    public Property resolveProperty(JavaType propType, ModelConverterContext context, Annotation[] annotations, Iterator<ModelConverter> next) {
        this.LOGGER.debug("resolveProperty {}", propType);
        Property property = null;
        if (propType.isContainerType()) {
            JavaType keyType = propType.getKeyType();
            JavaType valueType = propType.getContentType();
            if (keyType != null && valueType != null) {
                property = (new MapProperty()).additionalProperties(context.resolveProperty(valueType, new Annotation[0]));
            } else if (valueType != null) {
                Property items = context.resolveProperty(valueType, new Annotation[0]);
                if (annotations != null && annotations.length > 0) {
                    Annotation[] var9 = annotations;
                    int var10 = annotations.length;

                    for(int var11 = 0; var11 < var10; ++var11) {
                        Annotation annotation = var9[var11];
                        if (annotation instanceof XmlElement) {
                            XmlElement xmlElement = (XmlElement)annotation;
                            if (xmlElement != null && xmlElement.name() != null && !"".equals(xmlElement.name()) && !"##default".equals(xmlElement.name())) {
                                Xml xml = items.getXml() != null ? items.getXml() : new Xml();
                                xml.setName(xmlElement.name());
                                items.setXml(xml);
                            }
                        }
                    }
                }

                ArrayProperty arrayProperty = (new ArrayProperty()).items(items);
                if (this._isSetType(propType.getRawClass())) {
                    arrayProperty.setUniqueItems(true);
                }

                property = arrayProperty;
            }
        } else {
            property = PrimitiveType.createProperty(propType);
        }

        if (property == null) {
            if (propType.isEnumType()) {
                property = new StringProperty();
                this._addEnumProps(propType.getRawClass(), (StringProperty)property);
            } else if (this._isOptionalType(propType)) {
                property = context.resolveProperty(propType.containedType(0), (Annotation[])null);
            } else {
                Model innerModel = context.resolve(propType);
                if (innerModel instanceof ComposedModel) {
                    innerModel = ((ComposedModel)innerModel).getChild();
                }

                if (innerModel instanceof ModelImpl) {
                    ModelImpl mi = (ModelImpl)innerModel;
                    if (StringUtils.isNotEmpty(mi.getReference())) {
                        property = new RefProperty(mi.getReference());
                    } else {
                        property = new RefProperty(mi.getName(), RefFormat.INTERNAL);
                    }
                }
            }
        }

        return (Property)property;
    }

    private boolean _isOptionalType(JavaType propType) {
        return Arrays.asList("com.google.common.base.Optional", "java.util.Optional").contains(propType.getRawClass().getCanonicalName());
    }

    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> next) {
        return this.shouldIgnoreClass(type) ? null : this.resolve(this._mapper.constructType(type), context, next);
    }

    protected void _addEnumProps(Class<?> propClass, StringProperty property) {
        boolean useIndex = this._mapper.isEnabled(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        boolean useToString = this._mapper.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        Enum[] var6 = (Enum[])propClass.getEnumConstants();
        int var7 = var6.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            Enum<?> en = var6[var8];
            String n;
            if (useIndex) {
                n = String.valueOf(en.ordinal());
            } else if (useToString) {
                n = en.toString();
            } else {
                n = en.name();
            }

            property._enum(n);
        }

        if (!useIndex && !useToString) {
            property._enum(Arrays.asList(this._intr.findEnumValues(propClass, (Enum[])propClass.getEnumConstants(), (String[])property.getEnum().toArray(new String[0]))));
        }

    }
    public static String initialsTurnLowercase(String strValue){
        if(StringUtils.isEmpty(strValue)){
            return strValue;
        }
        int point = strValue.codePointAt(0);
        if(point < 65 || point > 90){
            return strValue;
        }
        char[] strCharArr = strValue.toCharArray();
        strCharArr[0] += 32;
        return String.valueOf(strCharArr);

    }
    public Model resolve(JavaType type, ModelConverterContext context, Iterator<ModelConverter> next) {
        if (!type.isEnumType() && PrimitiveType.fromType(type) == null) {
            BeanDescription beanDesc = this._mapper.getSerializationConfig().introspect(type);
            String name = this._typeName(type, beanDesc);
            if ("Object".equals(name)) {
                return new ModelImpl();
            } else {
                name = this.decorateModelName(context, name);
                Model resolvedModel = context.resolve(type.getRawClass());
                if (resolvedModel != null) {
                    if (!(resolvedModel instanceof ModelImpl) && !(resolvedModel instanceof ComposedModel) || resolvedModel instanceof ModelImpl && ((ModelImpl)resolvedModel).getName().equals(name)) {
                        return resolvedModel;
                    }

                    if (resolvedModel instanceof ComposedModel) {
                        Model childModel = ((ComposedModel)resolvedModel).getChild();
                        if (childModel != null && (!(childModel instanceof ModelImpl) || ((ModelImpl)childModel).getName().equals(name))) {
                            return resolvedModel;
                        }
                    }
                }

                ModelImpl model = (new ModelImpl()).type("object").name(name).description(this._description(beanDesc.getClassInfo()));
                if (!type.isContainerType()) {
                    context.defineModel(name, model, type, (String)null);
                }

                if (type.isContainerType()) {
                    context.resolve(type.getContentType());
                    return null;
                } else {
                    ApiModel apiModel = (ApiModel)beanDesc.getClassAnnotations().get(ApiModel.class);
                    if (apiModel != null && StringUtils.isNotEmpty(apiModel.reference())) {
                        model.setReference(apiModel.reference());
                    }

                    XmlRootElement rootAnnotation = (XmlRootElement)beanDesc.getClassAnnotations().get(XmlRootElement.class);
                    if (rootAnnotation != null && !"".equals(rootAnnotation.name()) && !"##default".equals(rootAnnotation.name())) {
                        this.LOGGER.debug("{}", rootAnnotation);
                        Xml xml = (new Xml()).name(rootAnnotation.name());
                        if (rootAnnotation.namespace() != null && !"".equals(rootAnnotation.namespace()) && !"##default".equals(rootAnnotation.namespace())) {
                            xml.namespace(rootAnnotation.namespace());
                        } else {
                            Package pkg = type.getRawClass().getPackage();
                            if (pkg != null) {
                                XmlSchema xmlSchma = (XmlSchema)pkg.getAnnotation(XmlSchema.class);
                                if (xmlSchma != null) {
                                    xml.namespace(xmlSchma.namespace());
                                }
                            }
                        }

                        model.xml(xml);
                    }

                    XmlAccessorType xmlAccessorTypeAnnotation = (XmlAccessorType)beanDesc.getClassAnnotations().get(XmlAccessorType.class);
                    JsonSerialize jasonSerialize = (JsonSerialize)beanDesc.getClassAnnotations().get(JsonSerialize.class);
                    if (jasonSerialize != null && jasonSerialize.as() != null) {
                        JavaType asType = this._mapper.constructType(jasonSerialize.as());
                        beanDesc = this._mapper.getSerializationConfig().introspect(asType);
                    }

                    Set<String> propertiesToIgnore = new HashSet();
                    JsonIgnoreProperties ignoreProperties = (JsonIgnoreProperties)beanDesc.getClassAnnotations().get(JsonIgnoreProperties.class);
                    if (ignoreProperties != null) {
                        propertiesToIgnore.addAll(Arrays.asList(ignoreProperties.value()));
                    }

                    String disc = apiModel == null ? "" : apiModel.discriminator();
                    if (disc.isEmpty()) {
                        JsonTypeInfo typeInfo = (JsonTypeInfo)beanDesc.getClassAnnotations().get(JsonTypeInfo.class);
                        if (typeInfo != null) {
                            disc = typeInfo.property();
                        }
                    }

                    if (!disc.isEmpty()) {
                        model.setDiscriminator(disc);
                    }

                    List<Property> props = new ArrayList();
                    Iterator var16 = beanDesc.findProperties().iterator();

                    while(true) {
                        Object property;
                        String propName;
                        Annotation[] annotations;
                        PropertyMetadata md;
                        Boolean isReadOnly;
                        AnnotatedMember member;
                        do {
                            do {
                                do {
                                    int offset;
                                    boolean currentTypeIsParentApiModelSubType;
                                    if (!var16.hasNext()) {
                                        Collections.sort(props, getPropertyComparator());
                                        Map<String, Property> modelProps = new LinkedHashMap();
                                        Iterator var44 = props.iterator();

                                        while(var44.hasNext()) {
                                            Property prop = (Property)var44.next();
                                            String pk = prop.getName();
                                            pk = initialsTurnLowercase(pk);
                                            modelProps.put(pk, prop);
                                        }

                                        model.setProperties(modelProps);
                                        Class<?> currentType = type.getRawClass();
                                        context.defineModel(name, model, currentType, (String)null);
                                        if (!this.resolveSubtypes(model, beanDesc, context)) {
                                            model.setDiscriminator((String)null);
                                        }

                                        if (apiModel != null) {
                                            Class<?> parentClass = apiModel.parent();
                                            if (parentClass != null && !parentClass.equals(Void.class) && !this.shouldIgnoreClass(parentClass)) {
                                                JavaType parentType = this._mapper.constructType(parentClass);
                                                BeanDescription parentBeanDesc = this._mapper.getSerializationConfig().introspect(parentType);
                                                boolean currentTypeIsParentSubType = false;
                                                List<NamedType> subTypes = this._intr.findSubtypes(parentBeanDesc.getClassInfo());
                                                if (subTypes != null) {
                                                    Iterator var56 = subTypes.iterator();

                                                    while(var56.hasNext()) {
                                                        NamedType subType = (NamedType)var56.next();
                                                        if (subType.getType().equals(currentType)) {
                                                            currentTypeIsParentSubType = true;
                                                            break;
                                                        }
                                                    }
                                                }

                                                currentTypeIsParentApiModelSubType = false;
                                                ApiModel parentApiModel = (ApiModel)parentBeanDesc.getClassAnnotations().get(ApiModel.class);
                                                if (parentApiModel != null) {
                                                    Class<?>[] apiModelSubTypes = parentApiModel.subTypes();
                                                    if (apiModelSubTypes != null) {
                                                        Class[] var64 = apiModelSubTypes;
                                                        offset = apiModelSubTypes.length;

                                                        for(int var66 = 0; var66 < offset; ++var66) {
                                                            Class<?> subType = var64[var66];
                                                            if (subType.equals(currentType)) {
                                                                currentTypeIsParentApiModelSubType = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (currentTypeIsParentSubType && currentTypeIsParentApiModelSubType) {
                                                    context.resolve(parentClass);
                                                    return context.resolve(currentType);
                                                }
                                            }
                                        }

                                        return model;
                                    }

                                    BeanPropertyDefinition propDef = (BeanPropertyDefinition)var16.next();
                                    property = null;
                                    propName = propDef.getName();
                                    annotations = null;
                                    String allowEmptyValue;
                                    if (propDef.getPrimaryMember() != null) {
                                        Member member1 = propDef.getPrimaryMember().getMember();
                                        JsonProperty jsonPropertyAnn = (JsonProperty)propDef.getPrimaryMember().getAnnotation(JsonProperty.class);
                                        if ((jsonPropertyAnn == null || !jsonPropertyAnn.value().equals(propName)) && member1 != null) {
                                            String altName = member1.getName();
                                            if (altName != null) {
                                                int length = altName.length();
                                                Iterator var25 = Arrays.asList("get", "is").iterator();

                                                while(var25.hasNext()) {
                                                    allowEmptyValue = (String)var25.next();
                                                    offset = allowEmptyValue.length();
                                                    if (altName.startsWith(allowEmptyValue) && length > offset && !Character.isUpperCase(altName.charAt(offset))) {
                                                        propName = altName;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    md = propDef.getMetadata();
                                    boolean hasSetter = false;
                                    currentTypeIsParentApiModelSubType = false;

                                    try {
                                        if (propDef.getSetter() == null) {
                                            hasSetter = false;
                                        } else {
                                            hasSetter = true;
                                        }
                                    } catch (IllegalArgumentException var36) {
                                        hasSetter = true;
                                    }

                                    JsonProperty pd;
                                    if (propDef.getGetter() != null) {
                                        pd = (JsonProperty)propDef.getGetter().getAnnotation(JsonProperty.class);
                                        if (pd != null) {
                                            currentTypeIsParentApiModelSubType = true;
                                        }
                                    }

                                    pd = null;
                                    if (!hasSetter & currentTypeIsParentApiModelSubType) {
                                        isReadOnly = Boolean.TRUE;
                                    } else {
                                        isReadOnly = Boolean.FALSE;
                                    }

                                    member = propDef.getPrimaryMember();
                                    allowEmptyValue = null;
                                } while(member == null);
                            } while(this.ignore(member, xmlAccessorTypeAnnotation, propName, propertiesToIgnore));

                            List<Annotation> annotationList = new ArrayList();
                            Iterator var28 = member.annotations().iterator();

                            while(var28.hasNext()) {
                                Annotation a = (Annotation)var28.next();
                                annotationList.add(a);
                            }

                            annotations = (Annotation[])annotationList.toArray(new Annotation[annotationList.size()]);
                        } while(this.hiddenByJsonView(annotations, context));

                        ApiModelProperty mp = (ApiModelProperty)member.getAnnotation(ApiModelProperty.class);
                        if (mp != null && mp.readOnly()) {
                            isReadOnly = mp.readOnly();
                        }

                        Boolean allowEmptyValue;
                        if (mp != null && mp.allowEmptyValue()) {
                            allowEmptyValue = mp.allowEmptyValue();
                        } else {
                            allowEmptyValue = null;
                        }

                        JavaType propType = member.getType(beanDesc.bindingsForBeanType());
                        if (mp != null && !mp.name().isEmpty()) {
                            propName = mp.name();
                        }

                        if (mp != null && !mp.dataType().isEmpty()) {
                            String or = mp.dataType();
                            JavaType innerJavaType = null;
                            this.LOGGER.debug("overriding datatype from {} to {}", propType, or);
                            if (or.toLowerCase().startsWith("list[")) {
                                String innerType = or.substring(5, or.length() - 1);
                                ArrayProperty p = new ArrayProperty();
                                Property primitiveProperty = PrimitiveType.createProperty(innerType);
                                if (primitiveProperty != null) {
                                    p.setItems(primitiveProperty);
                                } else {
                                    innerJavaType = this.getInnerType(innerType);
                                    p.setItems(context.resolveProperty(innerJavaType, annotations));
                                }

                                property = p;
                            } else if (or.toLowerCase().startsWith("map[")) {
                                int pos = or.indexOf(",");
                                if (pos > 0) {
                                    String innerType = or.substring(pos + 1, or.length() - 1);
                                    MapProperty p = new MapProperty();
                                    Property primitiveProperty = PrimitiveType.createProperty(innerType);
                                    if (primitiveProperty != null) {
                                        p.setAdditionalProperties(primitiveProperty);
                                    } else {
                                        innerJavaType = this.getInnerType(innerType);
                                        p.setAdditionalProperties(context.resolveProperty(innerJavaType, annotations));
                                    }

                                    property = p;
                                }
                            } else {
                                Property primitiveProperty = PrimitiveType.createProperty(or);
                                if (primitiveProperty != null) {
                                    property = primitiveProperty;
                                } else {
                                    innerJavaType = this.getInnerType(or);
                                    property = context.resolveProperty(innerJavaType, annotations);
                                }
                            }

                            if (innerJavaType != null) {
                                context.resolve(innerJavaType);
                            }
                        }
                        property = context.resolveProperty(propType, annotations);

                        if (property != null) {
                            ((Property)property).setName(propName);
                            if (mp != null && !mp.access().isEmpty()) {
                                ((Property)property).setAccess(mp.access());
                            }

                            Boolean required = md.getRequired();
                            if (required != null) {
                                ((Property)property).setRequired(required);
                            }

                            String description = this._intr.findPropertyDescription(member);
                            if (description != null && !"".equals(description)) {
                                ((Property)property).setDescription(description);
                            }

                            Integer index = this._intr.findPropertyIndex(member);
                            if (index != null) {
                                ((Property)property).setPosition(index);
                            }

                            ((Property)property).setDefault(this._findDefaultValue(member));
                            ((Property)property).setExample(this._findExampleValue(member));
                            ((Property)property).setReadOnly(this._findReadOnly(member));
                            if (allowEmptyValue != null) {
                                ((Property)property).setAllowEmptyValue(allowEmptyValue);
                            }

                            if (((Property)property).getReadOnly() == null && isReadOnly) {
                                ((Property)property).setReadOnly(isReadOnly);
                            }

                            Boolean readOnlyFromAccessMode = this._findReadOnlyFromAccessMode(member);
                            if (readOnlyFromAccessMode != null) {
                                ((Property)property).setReadOnly(readOnlyFromAccessMode);
                            }

                            if (mp != null) {
                                AllowableValues allowableValues = AllowableValuesUtils.create(mp.allowableValues());
                                if (allowableValues != null) {
                                    Map<PropertyId, Object> args = allowableValues.asPropertyArguments();
                                    PropertyBuilder.merge((Property)property, args);
                                }
                            }

                            if (mp != null && mp.extensions() != null) {
                                ((Property)property).getVendorExtensions().clear();
                                ((Property)property).getVendorExtensions().putAll(BaseReaderUtils.parseExtensions(mp.extensions()));
                            }

                            //JAXBAnnotationsHelper.apply(member, (Property)property);
                            this.applyBeanValidatorAnnotations((Property)property, annotations);
                            props.add((Property) property);
                        }
                    }
                }
            }
        } else {
            return null;
        }
    }

    protected String decorateModelName(ModelConverterContext context, String originalName) {
        String name = originalName;
        if (context.getJsonView() != null && context.getJsonView().value().length > 0) {
            String COMBINER = "-or-";
            StringBuffer sb = new StringBuffer();
            Class[] var6 = context.getJsonView().value();
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                Class<?> view = var6[var8];
                sb.append(view.getSimpleName()).append(COMBINER);
            }

            String suffix = sb.toString().substring(0, sb.length() - COMBINER.length());
            name = originalName + "_" + suffix;
        }

        return name;
    }

    private boolean hiddenByJsonView(Annotation[] annotations, ModelConverterContext context) {
        JsonView jsonView = context.getJsonView();
        if (jsonView == null) {
            return false;
        } else {
            Class<?>[] filters = jsonView.value();
            boolean containsJsonViewAnnotation = false;
            Annotation[] var6 = annotations;
            int var7 = annotations.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                Annotation ant = var6[var8];
                if (ant instanceof JsonView) {
                    containsJsonViewAnnotation = true;
                    Class<?>[] views = ((JsonView)ant).value();
                    Class[] var11 = filters;
                    int var12 = filters.length;

                    for(int var13 = 0; var13 < var12; ++var13) {
                        Class<?> f = var11[var13];
                        Class[] var15 = views;
                        int var16 = views.length;

                        for(int var17 = 0; var17 < var16; ++var17) {
                            Class<?> v = var15[var17];
                            if (v == f || v.isAssignableFrom(f)) {
                                return false;
                            }
                        }
                    }
                }
            }

            return containsJsonViewAnnotation;
        }
    }

    protected boolean ignore(Annotated member, XmlAccessorType xmlAccessorTypeAnnotation, String propName, Set<String> propertiesToIgnore) {
        if (propertiesToIgnore.contains(propName)) {
            return true;
        } else if (xmlAccessorTypeAnnotation == null) {
            return false;
        } else {
            return xmlAccessorTypeAnnotation.value().equals(XmlAccessType.NONE) && !member.hasAnnotation(XmlElement.class) && !member.hasAnnotation(XmlAttribute.class);
        }
    }

    private void handleUnwrapped(List<Property> props, Model innerModel, String prefix, String suffix) {
        if (StringUtils.isBlank(suffix) && StringUtils.isBlank(prefix)) {
            if (innerModel != null) {
                Map<String, Property> innerProps = innerModel.getProperties();
                if (innerProps != null) {
                    props.addAll(innerProps.values());
                }
            }
        } else {
            if (prefix == null) {
                prefix = "";
            }

            if (suffix == null) {
                suffix = "";
            }

            Iterator var5 = innerModel.getProperties().values().iterator();

            while(var5.hasNext()) {
                Property prop = (Property)var5.next();
                props.add(prop.rename(prefix + prop.getName() + suffix));
            }
        }

    }

    protected void applyBeanValidatorAnnotations(Property property, Annotation[] annotations) {
        Map<String, Annotation> annos = new HashMap();
        if (annotations != null) {
            Annotation[] var4 = annotations;
            int var5 = annotations.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                Annotation anno = var4[var6];
                annos.put(anno.annotationType().getName(), anno);
            }
        }

        if (annos.containsKey("javax.validation.constraints.NotNull")) {
            property.setRequired(true);
        }

        AbstractNumericProperty ap;
        if (annos.containsKey("javax.validation.constraints.Min") && property instanceof AbstractNumericProperty) {
            Min min = (Min)annos.get("javax.validation.constraints.Min");
            ap = (AbstractNumericProperty)property;
            ap.setMinimum(new BigDecimal(min.value()));
        }

        if (annos.containsKey("javax.validation.constraints.Max") && property instanceof AbstractNumericProperty) {
            Max max = (Max)annos.get("javax.validation.constraints.Max");
            ap = (AbstractNumericProperty)property;
            ap.setMaximum(new BigDecimal(max.value()));
        }

        StringProperty ap1;
        if (annos.containsKey("javax.validation.constraints.Size")) {
            Size size = (Size)annos.get("javax.validation.constraints.Size");
            if (property instanceof AbstractNumericProperty) {
                ap = (AbstractNumericProperty)property;
                ap.setMinimum(new BigDecimal(size.min()));
                ap.setMaximum(new BigDecimal(size.max()));
            } else if (property instanceof StringProperty) {
                ap1 = (StringProperty)property;
                ap1.minLength(new Integer(size.min()));
                ap1.maxLength(new Integer(size.max()));
            } else if (property instanceof ArrayProperty) {
                ArrayProperty sp = (ArrayProperty)property;
                sp.setMinItems(size.min());
                sp.setMaxItems(size.max());
            }
        }

        if (annos.containsKey("javax.validation.constraints.DecimalMin")) {
            DecimalMin min = (DecimalMin)annos.get("javax.validation.constraints.DecimalMin");
            if (property instanceof AbstractNumericProperty) {
                ap = (AbstractNumericProperty)property;
                ap.setMinimum(new BigDecimal(min.value()));
                ap.setExclusiveMinimum(!min.inclusive());
            }
        }

        if (annos.containsKey("javax.validation.constraints.DecimalMax")) {
            DecimalMax max = (DecimalMax)annos.get("javax.validation.constraints.DecimalMax");
            if (property instanceof AbstractNumericProperty) {
                ap = (AbstractNumericProperty)property;
                ap.setMaximum(new BigDecimal(max.value()));
                ap.setExclusiveMaximum(!max.inclusive());
            }
        }

        if (annos.containsKey("javax.validation.constraints.Pattern")) {
            Pattern pattern = (Pattern)annos.get("javax.validation.constraints.Pattern");
            if (property instanceof StringProperty) {
                ap1 = (StringProperty)property;
                ap1.setPattern(pattern.regexp());
            }
        }

    }

    protected JavaType getInnerType(String innerType) {
        try {
            Class<?> innerClass = ReflectionUtils.loadClassByName(innerType);
            if (innerClass != null) {
                TypeFactory tf = this._mapper.getTypeFactory();
                return tf.constructType(innerClass);
            }
        } catch (ClassNotFoundException var4) {
            var4.printStackTrace();
        }

        return null;
    }

    private boolean resolveSubtypes(ModelImpl model, BeanDescription bean, ModelConverterContext context) {
        List<NamedType> types = this._intr.findSubtypes(bean.getClassInfo());
        if (types == null) {
            return false;
        } else {
            this.removeSuperClassAndInterfaceSubTypes(types, bean);
            int count = 0;
            Class<?> beanClass = bean.getClassInfo().getAnnotated();
            Iterator var7 = types.iterator();

            while(true) {
                Class subtypeType;
                Model subtypeModel;
                do {
                    do {
                        if (!var7.hasNext()) {
                            return count != 0;
                        }

                        NamedType subtype = (NamedType)var7.next();
                        subtypeType = subtype.getType();
                    } while(!beanClass.isAssignableFrom(subtypeType));

                    subtypeModel = context.resolve(subtypeType);
                } while(!(subtypeModel instanceof ModelImpl));

                ModelImpl impl = (ModelImpl)subtypeModel;
                if (impl.getName().equals(model.getName())) {
                    impl.setName(this._typeNameResolver.nameForType(this._mapper.constructType(subtypeType), new Options[]{Options.SKIP_API_MODEL}));
                }

                Map<String, Property> baseProps = model.getProperties();
                Map<String, Property> subtypeProps = impl.getProperties();
                if (baseProps != null && subtypeProps != null) {
                    Iterator var14 = baseProps.entrySet().iterator();

                    while(var14.hasNext()) {
                        Entry<String, Property> entry = (Entry)var14.next();
                        if (((Property)entry.getValue()).equals(subtypeProps.get(entry.getKey()))) {
                            subtypeProps.remove(entry.getKey());
                        }
                    }
                }

                impl.setDiscriminator((String)null);
                ComposedModel child = (new ComposedModel()).parent(new RefModel(model.getName(), RefFormat.INTERNAL)).child(impl);
                context.defineModel(impl.getName(), child, subtypeType, (String)null);
                ++count;
            }
        }
    }

    private void removeSuperClassAndInterfaceSubTypes(List<NamedType> types, BeanDescription bean) {
        Class<?> beanClass = bean.getType().getRawClass();
        Class<?> superClass = beanClass.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            this.removeSuperSubTypes(types, superClass);
        }

        if (!types.isEmpty()) {
            Class<?>[] superInterfaces = beanClass.getInterfaces();
            Class[] var6 = superInterfaces;
            int var7 = superInterfaces.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                Class<?> superInterface = var6[var8];
                this.removeSuperSubTypes(types, superInterface);
                if (types.isEmpty()) {
                    break;
                }
            }
        }

    }

    private void removeSuperSubTypes(List<NamedType> resultTypes, Class<?> superClass) {
        JavaType superType = this._mapper.constructType(superClass);
        BeanDescription superBean = this._mapper.getSerializationConfig().introspect(superType);
        List<NamedType> superTypes = this._intr.findSubtypes(superBean.getClassInfo());
        if (superTypes != null) {
            resultTypes.removeAll(superTypes);
        }

    }

    private static enum GeneratorWrapper {
        PROPERTY(PropertyGenerator.class) {
            protected Property processAsProperty(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                return null;
            }

            protected Property processAsId(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                BeanDescription beanDesc = mapper.getSerializationConfig().introspect(type);
                Iterator var6 = beanDesc.findProperties().iterator();

                BeanPropertyDefinition def;
                String name;
                do {
                    if (!var6.hasNext()) {
                        return null;
                    }

                    def = (BeanPropertyDefinition)var6.next();
                    name = def.getName();
                } while(name == null || !name.equals(propertyName));

                AnnotatedMember propMember = def.getPrimaryMember();
                JavaType propType = propMember.getType(beanDesc.bindingsForBeanType());
                if (PrimitiveType.fromType(propType) != null) {
                    return PrimitiveType.createProperty(propType);
                } else {
                    return context.resolveProperty(propType, (Annotation[])Iterables.toArray(propMember.annotations(), Annotation.class));
                }
            }
        },
        INT(IntSequenceGenerator.class) {
            protected Property processAsProperty(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                Property id = new IntegerProperty();
                return GeneratorWrapper.process(id, propertyName, type, context);
            }

            protected Property processAsId(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                return new IntegerProperty();
            }
        },
        UUID(UUIDGenerator.class) {
            protected Property processAsProperty(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                Property id = new UUIDProperty();
                return GeneratorWrapper.process(id, propertyName, type, context);
            }

            protected Property processAsId(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                return new UUIDProperty();
            }
        },
        NONE(None.class) {
            protected Property processAsProperty(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                return null;
            }

            protected Property processAsId(String propertyName, JavaType type, ModelConverterContext context, ObjectMapper mapper) {
                return null;
            }
        };

        private final Class<? extends ObjectIdGenerator> generator;

        private GeneratorWrapper(Class<? extends ObjectIdGenerator> generator) {
            this.generator = generator;
        }

        protected abstract Property processAsProperty(String var1, JavaType var2, ModelConverterContext var3, ObjectMapper var4);

        protected abstract Property processAsId(String var1, JavaType var2, ModelConverterContext var3, ObjectMapper var4);

        public static Property processJsonIdentity(JavaType type, ModelConverterContext context, ObjectMapper mapper, JsonIdentityInfo identityInfo, JsonIdentityReference identityReference) {
            GeneratorWrapper wrapper = identityInfo != null ? getWrapper(identityInfo.generator()) : null;
            if (wrapper == null) {
                return null;
            } else {
                return identityReference != null && identityReference.alwaysAsId() ? wrapper.processAsId(identityInfo.property(), type, context, mapper) : wrapper.processAsProperty(identityInfo.property(), type, context, mapper);
            }
        }

        private static GeneratorWrapper getWrapper(Class<?> generator) {
            GeneratorWrapper[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                GeneratorWrapper value = var1[var3];
                if (value.generator.isAssignableFrom(generator)) {
                    return value;
                }
            }

            return null;
        }

        private static Property process(Property id, String propertyName, JavaType type, ModelConverterContext context) {
            id.setName(propertyName);
            Model model = context.resolve(type);
            if (model instanceof ComposedModel) {
                model = ((ComposedModel)model).getChild();
            }

            if (model instanceof ModelImpl) {
                ModelImpl mi = (ModelImpl)model;
                mi.getProperties().put(propertyName, id);
                return new RefProperty(StringUtils.isNotEmpty(mi.getReference()) ? mi.getReference() : mi.getName());
            } else {
                return null;
            }
        }


    }
}
