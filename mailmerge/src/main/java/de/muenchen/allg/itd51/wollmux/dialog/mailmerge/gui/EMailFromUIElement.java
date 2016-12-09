package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import java.util.Map;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#emailfrom}.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class EMailFromUIElement extends UIElement
{
  private final JTextField fromField;

  public EMailFromUIElement(String label, UIElementAction action,
      final String value, String group, final MailMergeParams mmp)
  {
    super(Box.createVerticalBox(), group);
    Box vbox = (Box) getCompo();

    Box hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(label));
    hbox.add(Box.createHorizontalStrut(5));
    this.fromField = new JTextField(mmp.getDefaultEmailFrom());
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(fromField);
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(5));
  }

  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    String from = fromField.getText();
    if (from.length() == 0)
      throw new InvalidArgumentException(
        L.m("Sie müssen eine Absenderadresse angeben!"));

    args.put(SubmitArgument.emailFrom, from);
  }
}