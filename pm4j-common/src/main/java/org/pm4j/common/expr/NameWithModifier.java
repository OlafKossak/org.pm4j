package org.pm4j.common.expr;

import java.util.HashSet;
import java.util.Set;

import org.pm4j.common.exception.CheckedExceptionWrapper;
import org.pm4j.common.expr.parser.ParseCtxt;
import org.pm4j.common.expr.parser.ParseException;

/**
 * Parses a name with its modifier specification.
 * <p>
 * Examples:
 * <pre>
 *   expression without modifier:         myVar
 *   expression for a variable:           #myVar
 *   expression for something optional (may be null):   (o)myVar
 *   expression for a field or getter that may exist: (x)myVar
 *   expression for a field or getter that may exist or may be null: (x,o)myVar.x
 *   expression for an optional variable: (o)#myVar
 * </pre>
 *
 * @author olaf boede
 */
public class NameWithModifier implements Cloneable {

  private Set<Modifier> modifiers = new HashSet<Modifier>();
  private boolean variable;
  private String name;

  public Set<Modifier> getModifiers() { return modifiers; }
  public boolean isOptional() { return modifiers.contains(Modifier.OPTIONAL); }
  public boolean isVariable() { return variable; }
  public void setVariable(boolean variable) { this.variable = variable; }
  public String getName() { return name; }

  @Override
  public NameWithModifier clone() {
    try {
      return (NameWithModifier) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new CheckedExceptionWrapper(e);
    }
  }

  public static enum Modifier {
    OPTIONAL("o") {
      @Override public void applyModifier(NameWithModifier n) {
        n.modifiers.add(OPTIONAL);
      }
    },
    EXISTS_OPTIONALLY("x") {
      @Override public void applyModifier(NameWithModifier n) {
        n.modifiers.add(EXISTS_OPTIONALLY);
      }
    },
    REPEATED("*") {
      @Override public void applyModifier(NameWithModifier n) {
        n.modifiers.add(REPEATED);
      }
    };

    private final String id;

    private Modifier(String id) {
      if (id.length() != 1) {
        throw new IllegalArgumentException("The parse algorithm needs to be extended to support more than one character.");
      }
      this.id = id;
    }

    public abstract void applyModifier(NameWithModifier n);

    static Modifier parse(ParseCtxt ctxt) {
      char ch = ctxt.skipBlanks().readCharAndAdvance();

      for (Modifier m : values()) {
        if (m.id.charAt(0) == ch) {
          return m;
        }
      }

      throw new ParseException(ctxt, "Unknown modifier '" + ch + "' found.");
    }

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (!modifiers.isEmpty()) {
      sb.append("(");
      for (Modifier m : Modifier.values()) {
        if (modifiers.contains(m)) {
          sb.append(m.id);
        }
      }
      sb.append(")");
    }

    if (variable) {
      sb.append('#');
    }

    sb.append(name);

    return sb.toString();
  }

  /**
   * Parses a name with modifiers.
   * <p>
   * Examples:
   * <ul>
   *   <li>'myName' - a simple name</li>
   *   <li>'#myVar' - a variable</li>
   *   <li>'(o)myName - an optional name</li>
   *   <li>'(o)#myName - an optional variable</li>
   * </ul>
   *
   * @param ctxt The current parse context.
   * @return The parsed syntax element.
   * @throws ParseException if the string at the current parse position is not a name.
   */
  public static NameWithModifier parseNameAndModifier(ParseCtxt ctxt) {
    NameWithModifier n = new NameWithModifier();

    ctxt.skipBlanks();
    if (ctxt.isOnChar('(')) {
      ctxt.readChar('(');

      boolean done = false;
      do {
        Modifier m = Modifier.parse(ctxt);
        m.applyModifier(n);

        ctxt.skipBlanks();
        switch (ctxt.currentChar()) {
          case ')': ctxt.readChar(')');
                    done = true;
                    break;
          case ',': ctxt.readChar(',');
                    break;
          default:  throw new ParseException(ctxt, "Can't interpret character '" +
                                             ctxt.currentChar() + "' in modifier list.");
        }
      }
      while(!done);

      ctxt.skipBlanks();
    }

    if (ctxt.readOptionalChar('#')) {
      n.setVariable(true);
    }

    n.name = ctxt.skipBlanksAndReadNameString();

    if (n.name == null) {
      throw new ParseException(ctxt, "Missing name string.");
    }

    return n;
  }


}