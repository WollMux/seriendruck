package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.util.Map;

/**
 * wird von {@link UIElement#addSubmitArgs(Map)} geschmissen, wenn die
 * Benutzereingaben unzureichend oder fehlerhaft sind.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class InvalidArgumentException extends Exception
{
  private static final long serialVersionUID = -2091420849047004341L;

  public InvalidArgumentException()
  {
    super(null, null);
  }

  public InvalidArgumentException(String msg)
  {
    super(msg);
  }
}