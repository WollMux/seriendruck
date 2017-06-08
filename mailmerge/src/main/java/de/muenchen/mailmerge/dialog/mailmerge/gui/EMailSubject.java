package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.util.Map;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#emailsubject}.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class EMailSubject extends UIElement
{
  private final JTextField textField;

  private final MailMergeParams mmp;

  public EMailSubject(String label, UIElementAction action, final String value,
      String group, final MailMergeParams mmp)
  {
    super(Box.createVerticalBox(), group);
    Box vbox = (Box) getCompo();

    Box hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(label));
    hbox.add(Box.createHorizontalStrut(5));
    this.textField = new JTextField("");
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(textField);
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(5));
    this.mmp = mmp;
  }

  @Override
  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    String subject = textField.getText();
    if (subject.trim().length() == 0)
    {
      int res =
        JOptionPane.showConfirmDialog(mmp.getDialog(),
          L.m("Ihre Betreffszeile ist leer. Möchten Sie wirklich fortsetzen?"),
          L.m("Betreff fehlt!"), JOptionPane.YES_NO_OPTION);
      if (res == JOptionPane.NO_OPTION)
        throw new InvalidArgumentException();
    }

    args.put(SubmitArgument.emailSubject, subject);
  }
}