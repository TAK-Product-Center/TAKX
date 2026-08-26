package gov.takx.platform.query;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.ShapeConstraint;
import gov.takx.platform.utils.ShapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Field-based constraint matching for RDMs.
 * <p>
 * Instances of this matcher are intended to be reused on arbitrary RDMs, so the constraints are compiled up front
 * to minimize overhead on subsequent {@link #matches(IRaptorDataMessage)} operations.
 *
 * @since 1.49
 */
public class MessageFieldMatcher extends AFieldMatcher<IRaptorDataMessage>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Construct a field matcher using the given list of constraints.
     *
     * @param constraints Constraints to match against
     */
    public MessageFieldMatcher(List<IConstraint> constraints)
    {
        super(constraints);
    }

    /**
     * Construct a field matcher using the given collection of lists of constraints.
     *
     * @param constraintSets Constraints to match against
     */
    public MessageFieldMatcher(Collection<List<IConstraint>> constraintSets)
    {
        super(constraintSets);
    }

    /**
     * Construct a field matcher using the given query constraints.
     *
     * @param qc Query constraints to match against
     */
    public MessageFieldMatcher(IQueryConstraints qc)
    {
        super(qc);
    }

    /**
     * Determine if the RDM matches all the configured constraints.
     *
     * @param rdm Message to test
     * @return {@code true} if all constraints are satisfied
     */
    public boolean matches(IRaptorDataMessage rdm)
    {
        return doMatch(rdm);
    }

    /**
     * Create the predicate to determine if the RDM matches a given ShapeConstraint.
     *
     * @param constraint Constraint to match
     * @return Predicate to test that the constraint matches
     */
    @Override
    protected Predicate<IRaptorDataMessage> compileShapeConstraint(ShapeConstraint constraint)
    {
        final ShapeConstraint.ShapeType shapeType = constraint.getShapeType();

        switch (shapeType)
        {
            case rectangle:
                final ILocation minCorner = constraint.getMinCorner();
                final ILocation maxCorner = constraint.getMaxCorner();

                return minCorner == null || maxCorner == null
                        ? rdm -> false
                        : rdm -> rdm.hasLocation() && ShapeUtils.polygonContainsLocation(Arrays.asList(
                                minCorner,
                                new RaptorLocation(minCorner.getLatitude(), maxCorner.getLongitude()),
                                maxCorner,
                                new RaptorLocation(maxCorner.getLatitude(), minCorner.getLongitude())),
                                rdm.getLocation());

            case circle:
                final ILocation center = constraint.getCenter();
                final double radius = constraint.getRadius();

                return rdm -> rdm.hasLocation() && ShapeUtils.circleContainsPoint(center, radius, rdm.getLocation());

            case polygon:
                final List<ILocation> vertices = Arrays.asList(constraint.getVertices());

                return rdm -> rdm.hasLocation() && ShapeUtils.polygonContainsLocation(vertices, rdm.getLocation());

            default:
                logger.error("Can't match ShapeConstraint; Unknown Shape Type: {}", shapeType);
                return rdm -> false;
        }
    }

    @Override
    protected Object getCoreFieldValue(ASimpleFieldSelector field, IRaptorDataMessage rdm)
    {
        return FieldExtractor.getCoreField(rdm, CoreField.valueOf(field.getFieldName()));
    }

    @Override
    protected Object getExtendedFieldValue(ASimpleFieldSelector field, IRaptorDataMessage rdm)
    {
        return rdm.getField(field.getFieldName());
    }

    /**
     * Optimized tests against core fields with primitive types.
     *
     * @param fc The Field Constraint to process
     * @return predicate
     * @since 2.30
     */
    @Override
    protected Predicate<IRaptorDataMessage> createCoreFieldComparison(FieldConstraint fc)
    {
        final ASimpleFieldSelector field = fc.getField();
        final CoreField coreField = CoreField.valueOf(field.getFieldName());
        final ComparisonType comparator = fc.getComparator();

        final boolean bv;
        final long lv;
        final double dv;

        switch (coreField)
        {
            case hasLocation:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.hasLocation()));
            case hasTime:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.hasTime()));
            case hasElevation:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.hasElevation()));
            case has3dOrientation:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.has3dOrientation()));
            case hasHeading:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.hasHeading()));
            case hasMotion:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.hasMotion()));
            case hasSpeed:
                bv = (Boolean) fc.getValue();
                return rdm -> comparator.interpretComparison(Boolean.compare(bv, rdm.hasSpeed()));

            case time:
                lv = ((Number) fc.getValue()).longValue();
                return rdm -> comparator.interpretComparison((Long.compare(rdm.getTime(), lv)));
            case receiveTime:
                lv = ((Number) fc.getValue()).longValue();
                return rdm -> comparator.interpretComparison((Long.compare(rdm.getReceiveTime(), lv)));
            case relevantTime:
                lv = ((Number) fc.getValue()).longValue();
                return rdm -> comparator.interpretComparison((Long.compare(rdm.getRelevantTime(), lv)));

            case latitude:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getLatitude(), dv)));
            case longitude:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getLongitude(), dv)));
            case elevation:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getElevation(), dv)));
            case heading:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getHeading(), dv)));
            case pitch:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getPitch(), dv)));
            case roll:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getRoll(), dv)));
            case speed:
                dv = ((Number) fc.getValue()).doubleValue();
                return rdm -> comparator.interpretComparison((Double.compare(rdm.getSpeedInMps(), dv)));

            default:
                return rdm -> testConstraint(this::getCoreFieldValue, fc, rdm);
        }
    }
}
