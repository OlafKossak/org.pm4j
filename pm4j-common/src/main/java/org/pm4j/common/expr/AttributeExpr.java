package org.pm4j.common.expr;

import org.pm4j.common.expr.NameWithModifier.Modifier;
import org.pm4j.common.expr.parser.ParseCtxt;
import org.pm4j.core.util.reflection.BeanAttrAccessor;
import org.pm4j.core.util.reflection.BeanAttrAccessorImpl;
import org.pm4j.core.util.reflection.ReflectionException;

/**
 * A path that uses a public field or getter/setter to resolve the path string.
 *
 * @author olaf boede
 */
public class AttributeExpr extends ExprBase<ExprExecCtxt> implements OptionalExpression {

  private NameWithModifier name;
  private BeanAttrAccessor accessor;

  public AttributeExpr(NameWithModifier name, BeanAttrAccessor accessor) {
    this(name);
    this.accessor = accessor;
  }

  public AttributeExpr(NameWithModifier name) {
    this.name = name;
  }

  @Override
  public Object execImpl(ExprExecCtxt ctxt) {
    BeanAttrAccessor accessor = ensureAccessor(ctxt);
    if (accessor == null) {
      return null;
    }

    Object currentObj = ctxt.getCurrentValue();

    if (currentObj == null) {
      if (!hasNameModifier(Modifier.OPTIONAL)) {
        throw new ExprExecExeption(ctxt, "Unable to resolve expression part '" + name + "' on a 'null' value.");
      } else {
        return null;
      }
    }

    return accessor.getBeanAttrValue(currentObj);
  }

  @Override
  protected void execAssignImpl(ExprExecCtxt ctxt, Object value) {
    ensureAccessor(ctxt).setBeanAttrValue(ctxt.getCurrentValue(), value);
  }

  @Override
  public boolean hasNameModifier(Modifier nameModifier) {
    return name.getModifiers().contains(nameModifier);
  }

  private BeanAttrAccessor ensureAccessor(ExprExecCtxt ctxt) {
    // FIXME: check in addition the used class of the accessor!
    if (accessor == null) {
      try {
        accessor = new BeanAttrAccessorImpl(ctxt.getCurrentValue().getClass(), name.getName());
      } catch (ReflectionException e) {
    	// Property does not exist. Check if that's ok.
    	// Consider the old meaning of 'o' which also worked like an 'x'.
        if(ParseCtxt.getSyntaxVersion() == SyntaxVersion.VERSION_1) {
          if (!(hasNameModifier(Modifier.OPTIONAL) || hasNameModifier(Modifier.EXISTS_OPTIONALLY))) {
              throw new ExprExecExeption(ctxt, "Unable to resolve expression part '" + name + "'.", e);
          }
        } else {
          if (!hasNameModifier(Modifier.EXISTS_OPTIONALLY)) {
              throw new ExprExecExeption(ctxt, "Unable to resolve expression part '" + name + "'.", e);
          }
        }
        return null;
      }
    }
    return accessor;
  }

  @Override
  public String toString() {
    return name.toString();
  }

  public static AttributeExpr parse(ParseCtxt ctxt) {
    NameWithModifier n = NameWithModifier.parseNameAndModifier(ctxt);
    return (n != null) ? new AttributeExpr(n) : null;
  }

}
