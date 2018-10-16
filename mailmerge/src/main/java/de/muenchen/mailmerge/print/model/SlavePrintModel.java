package de.muenchen.mailmerge.print.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyChangeListener;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.beans.XVetoableChangeListener;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.GlobalFunctions;
import de.muenchen.mailmerge.print.PrintFunction;

/**
 * Beim Aufruf einer einzelnen Druckfunktion wird dieser Druckfunktion ein
 * XPrintModel, repräsentiert durch das SlavePrintModel, übergeben. Für jede im
 * MaserPrintModel verwaltete und aufgerufene Druckfunktion existiert also genau
 * ein SlavePrintModel, das im wesentlichen alle Anfragen (Methodenaufrufe) an das
 * MasterPrintModel weiterleitet. Das SlavePrintModel kennt seine Position (idx) in
 * der Aufrufkette und sorgt vor allem dafür, dass beim Aufruf von printWithProps()
 * die nächste Druckfunktion der Aufrufkette gestartet wird.
 *
 * Das SlavePrintModel ist von WeakBase abgeleitet, damit es in der Druckfunktion
 * mit den UNO-Mitteln inspiziert werden kann.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
class SlavePrintModel extends WeakBase implements XPrintModel,
    InternalPrintModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SlavePrintModel.class);

  private int idx;

  private MasterPrintModel master;

  /**
   * Enthält die Beschreibung des Druckvorgangs, der in diesem SlavePrintModel
   * bearbeitet wird.
   */
  private String stage;

  /**
   * Erzeugt ein neues SlavePrintModel, das in der Aufrufkette, die durch das
   * MasterPrintModel master verwaltet wird, an der Stelle idx steht.
   *
   * @param master
   *          Das MasterPrintModel, an das die meisten Anfragen weitergeleitet
   *          werden und das die Aufrufkette der Druckfunktionen verwaltet.
   * @param idx
   *          Die Position der zu diesem SlavePrintModel zugehörigen Druckfunktion
   *          in der Aufrufkette von master.
   */
  public SlavePrintModel(MasterPrintModel master, int idx)
  {
    this.master = master;
    this.idx = idx;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getTextDocument()
   */
  @Override
  public XTextDocument getTextDocument()
  {
    return master.getTextDocument();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
   */
  @Override
  public void print(short numberOfCopies)
  {
    for (int i = 0; i < numberOfCopies && !isCanceled(); ++i)
      printWithProps();
  }

  /**
   * Diese Methode ist die wichtigste Methode im SlavePrintModel, denn sie sorgt
   * dafür, dass beim Aufruf von PrintWithProps die Weiterleitung an die nächste
   * Druckfunktion der Aufrufkette veranlasst wird.
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printWithProps()
   */
  @Override
  public void printWithProps()
  {
    if (isCanceled())
      return;

    PrintFunction f = master.getPrintFunction(idx + 1);
    if (f != null)
    {
      XPrintModel pmod = new SlavePrintModel(master, idx + 1);
      Thread t = f.invoke(pmod);
      try
      {
        t.join();
      }
      catch (InterruptedException e)
      {
        LOGGER.error("", e);
      }
      master.setPrintProgressMaxValue(pmod, (short) 0);
    }
    else
    {
      master.finalPrint();
    }
    if (stage != null)
      master.setStage(stage);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
   * java.lang.String)
   */
  @Override
  public void setFormValue(String arg0, String arg1)
  {
    master.setFormValue(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getDocumentModified()
   */
  @Override
  public boolean getDocumentModified()
  {
    return master.getDocumentModified();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setDocumentModified(boolean)
   */
  @Override
  public void setDocumentModified(boolean arg0)
  {
    master.setDocumentModified(arg0);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#collectNonWollMuxFormFields()
   */
  @Override
  public void collectNonWollMuxFormFields()
  {
    master.collectNonWollMuxFormFields();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintBlocksProps(java.lang.String
   * , boolean, boolean)
   */
  @Override
  public void setPrintBlocksProps(String arg0, boolean arg1, boolean arg2)
  {
    master.setPrintBlocksProps(arg0, arg1, arg2);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.sun.star.beans.XPropertySet#getPropertySetInfo()
   */
  @Override
  public XPropertySetInfo getPropertySetInfo()
  {
    return master.getPropertySetInfo();
  }

  /*
   * (non-Javadoc)
   *
   * @see com.sun.star.beans.XPropertySet#setPropertyValue(java.lang.String,
   * java.lang.Object)
   */
  @Override
  public void setPropertyValue(String key, Object val)
      throws UnknownPropertyException, PropertyVetoException,
      WrappedTargetException
  {
    if (PrintModels.STAGE.equalsIgnoreCase(key))
    {
      if (val != null)
      {
        stage = val.toString();
        master.setStage(stage);
      }
    }
    else
      master.setPropertyValue(key, val);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.sun.star.beans.XPropertySet#getPropertyValue(java.lang.String)
   */
  @Override
  public Object getPropertyValue(String arg0) throws UnknownPropertyException,
      WrappedTargetException
  {
    return master.getPropertyValue(arg0);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getProp(java.lang.String,
   * java.lang.Object)
   */
  @Override
  public Object getProp(String arg0, Object arg1)
  {
    return master.getProp(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.sun.star.beans.XPropertySet#addPropertyChangeListener(java.lang.String,
   * com.sun.star.beans.XPropertyChangeListener)
   */
  @Override
  public void addPropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    master.addPropertyChangeListener(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.sun.star.beans.XPropertySet#removePropertyChangeListener(java.lang.String,
   * com.sun.star.beans.XPropertyChangeListener)
   */
  @Override
  public void removePropertyChangeListener(String arg0,
      XPropertyChangeListener arg1) throws UnknownPropertyException,
      WrappedTargetException
  {
    master.removePropertyChangeListener(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.sun.star.beans.XPropertySet#addVetoableChangeListener(java.lang.String,
   * com.sun.star.beans.XVetoableChangeListener)
   */
  @Override
  public void addVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    master.addVetoableChangeListener(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.sun.star.beans.XPropertySet#removeVetoableChangeListener(java.lang.String,
   * com.sun.star.beans.XVetoableChangeListener)
   */
  @Override
  public void removeVetoableChangeListener(String arg0,
      XVetoableChangeListener arg1) throws UnknownPropertyException,
      WrappedTargetException
  {
    master.removeVetoableChangeListener(arg0, arg1);
  }

  /**
   * Der wesentliche Unterschied zur gleichnamigen Methode des Masters ist es, dass
   * nur Druckfunktionen angenommen werden, deren ORDER-Wert höher als der
   * ORDER-Wert der aktuellen Druckfunktion ist.
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#usePrintFunction(java.lang.String)
   */
  @Override
  public void usePrintFunction(String functionName) throws NoSuchMethodException
  {
    PrintFunction newFunc =
        GlobalFunctions.getInstance().getGlobalPrintFunctions().get(functionName);
    if (newFunc != null)
      useInternalPrintFunction(newFunc);
    else
      throw new NoSuchMethodException(L.m("Druckfunktion '%1' nicht definiert.",
        functionName));
  }

  /**
   * Der wesentliche Unterschied zur gleichnamigen Methode des Masters ist es, dass
   * nur Druckfunktionen angenommen werden, deren ORDER-Wert höher als der
   * ORDER-Wert der aktuellen Druckfunktion ist.
   *
   * @see de.muenchen.mailmerge.print.model.InternalPrintModel#useInternalPrintFunction(de.muenchen.mailmerge.print.PrintFunction)
   */
  @Override
  public boolean useInternalPrintFunction(PrintFunction function)
  {
    if (function != null)
    {
      PrintFunction currentFunc = master.getPrintFunction(idx);
      if (function.compareTo(currentFunc) <= 0)
      {
        LOGGER.error(L.m(
          "Druckfunktion '%1' muss einen höheren ORDER-Wert besitzen als die Druckfunktion '%2'",
          function.getFunctionName(), currentFunc.getFunctionName()));
        return false;
      }
      else
        return master.useInternalPrintFunction(function);
    }
    else
    {
      LOGGER.error(L.m("Die angeforderte interne Druckfunktion ist ungültig."));
      return false;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.XPrintModel#setGroupVisible(java.lang.String,
   * boolean)
   */
  @Override
  public void setGroupVisible(String arg0, boolean arg1)
  {
    master.setGroupVisible(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#isCanceled()
   */
  @Override
  public boolean isCanceled()
  {
    return master.isCanceled();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#cancel()
   */
  @Override
  public void cancel()
  {
    master.cancel();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintProgressMaxValue(short)
   */
  @Override
  public void setPrintProgressMaxValue(short maxValue)
  {
    master.setPrintProgressMaxValue(this, maxValue);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintProgressValue(short)
   */
  @Override
  public void setPrintProgressValue(short value)
  {
    master.setPrintProgressValue(this, value);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setPrintMessage(string)
   */
  @Override
  public void setPrintMessage(String value)
  {
    master.setPrintMessage(value);
  }
}