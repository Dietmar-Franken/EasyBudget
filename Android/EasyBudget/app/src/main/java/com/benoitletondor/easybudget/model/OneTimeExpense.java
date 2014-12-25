package com.benoitletondor.easybudget.model;

import java.util.Date;

/**
 * Created by benoit on 25/12/14.
 */
public class OneTimeExpense extends Expense
{
    private Date date;

// ------------------------------------>

    public OneTimeExpense(int amount, Date date)
    {
        super(amount);

        if( date == null )
        {
            throw new NullPointerException("date==null");
        }

        this.date = date;
    }

// ------------------------------------>

    public Date getDate()
    {
        return date;
    }
}
