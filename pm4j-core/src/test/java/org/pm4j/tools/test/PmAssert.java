package org.pm4j.tools.test;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.pm4j.core.pm.PmAttr;
import org.pm4j.core.pm.PmCommand;
import org.pm4j.core.pm.PmCommand.CommandState;
import org.pm4j.core.pm.PmMessage;
import org.pm4j.core.pm.PmMessage.Severity;
import org.pm4j.core.pm.PmObject;
import org.pm4j.core.pm.api.PmMessageUtil;
import org.pm4j.core.pm.impl.PmInitApi;
import org.pm4j.core.pm.impl.PmUtil;

/**
 * A set of junit test support methods.
 *
 * @author olaf boede
 */
public class PmAssert {

  private PmAssert() {
  }

  public static void assertNoMessages(PmObject pm) {
    assertNoMessages("Unexpected error messages found.", pm);
  }

  public static void assertNoMessages(PmObject pm, Severity minSeverity) {
    assertNoMessages("Unexpected messages found.", pm, minSeverity);
  }

  public static void assertNoMessages(String msg, PmObject pm) {
    assertNoMessages(msg, pm, Severity.INFO);
  }

  public static void assertNoMessages(String msg, PmObject pm, Severity minSeverity) {
    String errMsgs = messagesToString(pm, minSeverity);
    if (StringUtils.isNotBlank(errMsgs)) {
      Assert.fail(msg + " " + pm.getPmRelativeName() + ": " + errMsgs);
    }
  }

  public static void assertNoConversationMessages(PmObject pm) {
    assertNoMessages(pm.getPmConversation());
  }

  public static void assertNoConversationMessages(String msg, PmObject pm) {
    assertNoMessages(msg, pm.getPmConversation());
  }

  public static void assertEnabled(PmObject... pms) {
    for (PmObject pm : pms) {
      if (!pm.isPmEnabled()) {
        fail(pm.getPmRelativeName() + " should be enabled.");
      }
    }
  }

  public static void assertNotEnabled(PmObject... pms) {
    for (PmObject pm : pms) {
      if (pm.isPmEnabled()) {
        fail(pm.getPmRelativeName() + " should not be enabled.");
      }
    }
  }

  public static void assertVisible(PmObject... pms) {
    for (PmObject pm : pms) {
      if (!pm.isPmVisible()) {
        fail(pm.getPmRelativeName() + " should be visible.");
      }
    }
  }

  public static void assertNotVisible(PmObject... pms) {
    for (PmObject pm : pms) {
      if (pm.isPmVisible()) {
        fail(pm.getPmRelativeName() + " should not be visible.");
      }
    }
  }

  public static <T> void setValue(PmAttr<T> attr, T value) {
    assertEnabled(attr);
    attr.setValue(value);
    assertNoMessages(attr);
    assertEquals(value, attr.getValue());
  }

  public static void setValueAsString(PmAttr<?> attr, String value) {
    assertEnabled(attr);
    attr.setValueAsString(value);
    assertNoMessages(attr);
    assertEquals(value, attr.getValueAsString());
  }

  public static void doIt(PmCommand cmd) {
    doIt(cmd.getPmRelativeName(), cmd, CommandState.EXECUTED);
  }

  public static void doIt(String msg, PmCommand cmd) {
    doIt(msg, cmd, CommandState.EXECUTED);
  }

  public static void doIt(PmCommand cmd, CommandState expectedState) {
    doIt(cmd.getPmRelativeName(), cmd, expectedState);
  }

  public static void doIt(String msg, PmCommand cmd, CommandState expectedState) {
    assertEnabled(cmd);
    CommandState execState = cmd.doIt().getCommandState();
    if (execState != expectedState) {
      String msgPfx = StringUtils.isEmpty(msg) ? cmd.getPmRelativeName() : msg;
      Assert.assertEquals(msgPfx + messagesToString(" Messages: ", cmd, Severity.WARN), expectedState, execState);
    }
  }

  public static void initPmTree(PmObject rootPm) {
    PmInitApi.ensurePmInitialization(rootPm);
    for (PmObject pm : PmUtil.getPmChildren(rootPm)) {
      initPmTree(pm);
    }
  }

  private static String messagesToString(PmObject pm, Severity minSeverity) {
    return messagesToString(null, pm, minSeverity);
  }

  private static String messagesToString(String msgPrefix, PmObject pm, Severity minSeverity) {
    List<PmMessage> mlist = PmMessageUtil.getSubTreeMessages(pm, minSeverity);
    if (mlist.isEmpty()) {
      return "";
    } else {
      StringBuilder sb = new StringBuilder();
      for (PmMessage m : mlist) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(m.getPm().getPmRelativeName()).append(": ").append(m);
      }
      return StringUtils.defaultString(msgPrefix) + sb.toString();
    }
  }

}
