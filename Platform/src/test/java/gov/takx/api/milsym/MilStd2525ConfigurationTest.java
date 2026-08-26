package gov.takx.api.milsym;

import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.dataservices.shapes.Arrow;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Validation of {@link MilStd2525Configuration} and associated builder.
 * @since 5.1
 */
public class MilStd2525ConfigurationTest extends AContainerTest
{
    private static final String POINT_CODE = "SFPU--------***";
    private static final String LINE_CODE = "G*G*PA----****X";
    private static final String CIRCLE_CODE = "G*F*ACVC--****X";
    private static final String RECTANGLE_CODE = "G*F*LTF---****X";
    private static final String UNKNOWN_CODE = "G*G*OA----****X";

    @Test
    void builder_withValidConfig_buildsSuccessfully()
    {
        final Consumer<IShapeMapObject<?>> initializer = mo -> {
        };
        final Circle circle = new Circle();
        final String name = "my circle";

        final var configuration = MilStd2525Configuration.builder(CIRCLE_CODE)
                .name(name)
                .shape(circle)
                .withModifier("T", "T value")
                .withModifier("W", "W value")
                .initializer(initializer)
                .build();

        assertThat(configuration).isNotNull();
        assertThat(configuration.symbolCode()).isEqualTo(CIRCLE_CODE);
        assertThat(configuration.name()).isEqualTo(name);
        assertThat(configuration.shape()).isSameAs(circle);
        assertThat(configuration.initializer()).isSameAs(initializer);
        assertThat(configuration.modifiers()).hasSize(2)
                .containsExactlyInAnyOrder(new MilStd2525Configuration.ModifierValue("T", "T value"),
                        new MilStd2525Configuration.ModifierValue("W", "W value"));
    }

    @Test
    void builder_withNoNameSet_defaultsToShapeName()
    {
        final Circle circle = new Circle();
        circle.setName("my shape's name");

        final var configuration = MilStd2525Configuration.builder(CIRCLE_CODE).shape(circle).build();

        assertThat(configuration.name()).isEqualTo("my shape's name");
    }

    @Test
    void builder_withInvalidCode_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(UNKNOWN_CODE).build())
                .withMessageContaining("Unknown symbol");
    }

    @Test
    void builder_withNullShape_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(POINT_CODE).build())
                .withMessageContaining("Property \"shape\" has not been set");
    }

    @Test
    void builder_withUnsupportedShape_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(POINT_CODE).shape(new Arrow()).build())
                .withMessageContaining("Unsupported shape type");
    }

    @Test
    void builder_withWrongShape_throws()
    {
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(LINE_CODE).shape(new Square()).build())
                .withMessageContaining("does not support a shape of Square");
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(RECTANGLE_CODE).shape(new Circle()).build())
                .withMessageContaining("does not support a shape of Circle");
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(CIRCLE_CODE).shape(new Rectangle()).build())
                .withMessageContaining("does not support a shape of Rectangle");
        assertThatIllegalStateException()
                .isThrownBy(() -> MilStd2525Configuration.builder(POINT_CODE).shape(new Polyline()).build())
                .withMessageContaining("does not support a shape of Polyline");
    }

    @Test
    void builder_withUnknownModifier_throws()
    {
        final MilStd2525Configuration.Builder builder = MilStd2525Configuration.builder(CIRCLE_CODE)
                .shape(new Circle())
                .withModifier("XX", "not a valid modifier");

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("Unknown modifier id");
    }

    @Test
    void builder_withWrongModifier_throws()
    {
        final MilStd2525Configuration.Builder builder = MilStd2525Configuration.builder(CIRCLE_CODE)
                .shape(new Circle())
                .withModifier("X", "some value");

        assertThatIllegalStateException()
                .isThrownBy(builder::build)
                .withMessageContaining("does not support a modifier with");
    }
}
