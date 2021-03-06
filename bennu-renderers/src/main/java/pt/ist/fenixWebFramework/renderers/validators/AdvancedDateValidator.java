/**
 * 
 */
package pt.ist.fenixWebFramework.renderers.validators;

import java.text.ParseException;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.utl.ist.fenix.tools.util.DateFormatUtil;

import com.google.common.base.Predicate;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
public class AdvancedDateValidator extends DateValidator {

    private String validationPeriod;

    public AdvancedDateValidator() {
        super();
    }

    public AdvancedDateValidator(HtmlChainValidator htmlChainValidator) {
        super(htmlChainValidator);
    }

    public AdvancedDateValidator(HtmlChainValidator htmlChainValidator, String dateFormat) {
        super(htmlChainValidator, dateFormat);
    }

    @Override
    public void performValidation() {
        super.performValidation();

        if (isValid()) {
            try {
                DateTime dateTime = new DateTime(DateFormatUtil.parse(getDateFormat(), getComponent().getValue()).getTime());
                setValid(getValidationPeriodType().evaluateDate(dateTime));
            } catch (ParseException e) {
                setValid(false);
                e.printStackTrace();
            }
        }

    }

    public String getValidationPeriod() {
        return validationPeriod;
    }

    public void setValidationPeriod(String validationPeriod) {
        this.validationPeriod = validationPeriod;
        setMessage("renderers.validator.advancedDate." + getValidationPeriod());
    }

    public ValidationPeriodType getValidationPeriodType() {
        if (this.validationPeriod != null) {
            return ValidationPeriodType.valueOf(getValidationPeriod().toUpperCase());
        }
        return null;
    }

    private static Predicate<DateTime> pastPredicate = new Predicate<DateTime>() {
        @Override
        public boolean apply(DateTime dateTime) {
            return dateTime.isBeforeNow();
        }
    };

    private static Predicate<DateTime> pastOrTodayPredicate = new Predicate<DateTime>() {
        @Override
        public boolean apply(DateTime dateTime) {
            return dateTime.isBeforeNow() || dateTime.toLocalDate().isEqual(new LocalDate());
        }
    };

    private static Predicate<DateTime> futurePredicate = new Predicate<DateTime>() {
        @Override
        public boolean apply(DateTime dateTime) {
            return dateTime.isAfterNow();
        }
    };

    private enum ValidationPeriodType {

        PAST(pastPredicate), PASTORTODAY(pastOrTodayPredicate), FUTURE(futurePredicate);

        private Predicate<DateTime> predicate;

        private ValidationPeriodType(Predicate<DateTime> predicate) {
            this.predicate = predicate;
        }

        protected boolean evaluateDate(DateTime dateTime) {
            return this.predicate.apply(dateTime);
        }

    }

}
