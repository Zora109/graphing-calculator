package graphingcalculator;

import edu.macalester.graphics.CanvasWindow;
import edu.macalester.graphics.Line;
import edu.macalester.graphics.events.ModifierKey;
import edu.macalester.graphics.Point;
import edu.macalester.graphics.ui.Button;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class GraphingCalculator {
    private final CanvasWindow canvas;
    private final List<FunctionPlot> plots;
    private Point origin;
    private double scale;
    private double xmin, xmax, step;
    private double animationParameter;
    private Line xaxis, yaxis;
    private boolean animating = true;
    private double animationSpeed = 0.01;

    public GraphingCalculator(int width, int height) {
        canvas = new CanvasWindow("Graphing Calculator", width, height);
        plots = new ArrayList<>();

        origin = canvas.getCenter();
        scale = Math.min(width, height) / 4.0;

        xaxis = createAxisLine();
        yaxis = createAxisLine();

        coordinatesChanged();

        // Zoom buttons
        Button zoomIn = new Button("Zoom In");
        Button zoomOut = new Button("Zoom Out");
        zoomIn.setCenter(50, 20);
        zoomOut.setCenter(130, 20);
        canvas.add(zoomIn);
        canvas.add(zoomOut);

        zoomIn.onClick(() -> setScale(getScale() * 1.5));
        zoomOut.onClick(() -> setScale(getScale() / 1.5));

        // 拖拽：shift平移视图，否则控制动画参数（带惯性）
        canvas.onDrag(event -> {
            if (event.getModifiers().contains(ModifierKey.SHIFT)) {
                setOrigin(origin.add(event.getDelta()));
            } else {
                double delta = event.getDelta().getX() / width;
                setAnimationParameter(getAnimationParameter() + delta);
                animationSpeed = (animationSpeed + delta) / 2;
            }
        });

        // 按下暂停，松开恢复
        canvas.onMouseDown(event -> animating = false);
        canvas.onMouseUp(event -> animating = true);

        // 动画循环
        canvas.animate(() -> {
            if (animating) {
                setAnimationParameter(getAnimationParameter() + animationSpeed);
            }
        });
    }

    public void show(SimpleFunction function) {
        show((x, n) -> function.evaluate(x));
    }

    public void show(ParametricFunction function) {
        FunctionPlot plot = new FunctionPlot(function);
        plots.add(plot);
        canvas.add(plot.getGraphics());

        recolorPlots();
        recalculate(plot);
    }

    public double getAnimationParameter() {
        return animationParameter;
    }

    public void setAnimationParameter(double animationParameter) {
        this.animationParameter = animationParameter;
        recalculateAll();
    }

    public Point getOrigin() {
        return origin;
    }

    public void setOrigin(Point origin) {
        this.origin = origin;
        coordinatesChanged();
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
        coordinatesChanged();
    }

    private Line createAxisLine() {
        Line axis = new Line(0, 0, 0, 0);
        axis.setStrokeWidth(0.25);
        axis.setStrokeColor(new Color(0xA1A1A1));
        canvas.add(axis);
        return axis;
    }

    private void coordinatesChanged() {
        xaxis.setStartPosition(0, origin.getY());
        xaxis.setEndPosition(canvas.getWidth(), origin.getY());
        yaxis.setStartPosition(origin.getX(), 0);
        yaxis.setEndPosition(origin.getX(), canvas.getHeight());

        xmin = convertToEquationCoordinates(Point.ORIGIN).getX();
        xmax = convertToEquationCoordinates(new Point(canvas.getWidth(), 0)).getX();
        step = 2 / scale;

        recalculateAll();
    }

    private void recalculateAll() {
        plots.forEach(this::recalculate);
    }

    private void recalculate(FunctionPlot plot) {
        plot.recalculate(animationParameter, xmin, xmax, step, this::convertToScreenCoordinates);
    }

    private Point convertToScreenCoordinates(Point equationPoint) {
        return equationPoint.scale(scale, -scale).add(origin);
    }

    private Point convertToEquationCoordinates(Point screenPoint) {
        return screenPoint.subtract(origin).scale(1 / scale, -1 / scale);
    }

    private void recolorPlots() {
        int index = 0;
        for (FunctionPlot plot : plots) {
            plot.setColor(index, plots.size());
            index++;
        }
    }

    public static void main(String[] args) {
        GraphingCalculator calc = new GraphingCalculator(800, 600);

        for (int n = 1; n < 12; n++) {
            double base = n * 0.1 + 1.5;
            calc.show((x, t) -> {
                double result = 0;
                for (int i = 1; i < 20; i++) {
                    result += Math.sin(x * Math.pow(base, i) - t * i * 3)
                              / Math.pow(base, i);
                }
                return result;
            });
        }
    }
}