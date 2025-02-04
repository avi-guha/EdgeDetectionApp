package Detection;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import java.util.*;
import java.util.List;

public class EdgeDetection {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    /**
     * Processes the image at the given path by applying edge detection.
     * It draws a closed boundary on the image and saves the output.
     * @param imagePath The path to the input image.
     * @return The processed image as a Mat.
     */
    public static Mat processImage(String imagePath) {
        Mat original = Imgcodecs.imread(imagePath);
        if (original.empty()) {
            System.out.println("Error: Could not load the image!");
            return null;
        }

        // 1. Blur the image
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(original, blurred, new Size(15, 15), 0);

        // 2. Convert to HSV
        Mat hsv = new Mat();
        Imgproc.cvtColor(blurred, hsv, Imgproc.COLOR_BGR2HSV);

        // 3. Create masks for purple and orange colors
        Scalar lowerPurple = new Scalar(120, 40, 40);
        Scalar upperPurple = new Scalar(170, 255, 255);
        Scalar lowerOrange = new Scalar(0, 40, 40);
        Scalar upperOrange = new Scalar(30, 255, 255);

        Mat purpleMask = new Mat();
        Mat orangeMask = new Mat();
        Core.inRange(hsv, lowerPurple, upperPurple, purpleMask);
        Core.inRange(hsv, lowerOrange, upperOrange, orangeMask);

        // 4. Handle red wrapping in the orange range
        Mat redWrapMask = new Mat();
        Core.inRange(hsv, new Scalar(170, 40, 40), new Scalar(180, 255, 255), redWrapMask);
        Core.bitwise_or(orangeMask, redWrapMask, orangeMask);

        // 5. Edge detection for each mask
        Mat purpleEdges = new Mat();
        Mat orangeEdges = new Mat();
        Imgproc.Canny(purpleMask, purpleEdges, 50, 150);
        Imgproc.Canny(orangeMask, orangeEdges, 50, 150);

        // 6. Combine edges
        Mat boundaryEdges = new Mat();
        Core.bitwise_or(purpleEdges, orangeEdges, boundaryEdges);

        // 7. Morphological closing to bridge gaps
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(boundaryEdges, boundaryEdges, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), 2);

        // 8. Additional dilation to help close the top edge of the square
        Mat dilationKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(boundaryEdges, boundaryEdges, dilationKernel);

        // 9. Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(boundaryEdges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.isEmpty()) {
            System.out.println("No boundary found!");
            return original;
        }

        // 10. Choose the longest contour by perimeter
        MatOfPoint longestContour = getLongestContourByPerimeter(contours);
        List<Point> boundaryPoints = longestContour.toList();

        // 11. Filter near-duplicate points
        Set<String> visited = new LinkedHashSet<>();
        List<Point> filteredPoints = new ArrayList<>();
        double pixelTolerance = 0.5; // Half-pixel tolerance
        for (Point p : boundaryPoints) {
            long x = Math.round(p.x);
            long y = Math.round(p.y);
            String key = x + "_" + y;
            boolean isNearby = false;
            for (Point existing : filteredPoints) {
                if (Math.abs(existing.x - x) < pixelTolerance &&
                        Math.abs(existing.y - y) < pixelTolerance) {
                    isNearby = true;
                    break;
                }
            }
            if (!isNearby) {
                visited.add(key);
                filteredPoints.add(new Point(x, y));
            }
        }

        // 12. Ensure the contour is closed by connecting the first and last points if needed
        boolean isClosed = isContourClosed(boundaryPoints, 5.0);
        if (isClosed && !filteredPoints.isEmpty()) {
            Point first = filteredPoints.get(0);
            Point last = filteredPoints.get(filteredPoints.size() - 1);
            if (distance(first, last) > 5.0) {
                filteredPoints.add(new Point(first.x, first.y));
            }
        }

        // 13. Draw the final boundary as a closed polyline
        MatOfPoint finalContour = new MatOfPoint();
        finalContour.fromList(filteredPoints);
        Imgproc.polylines(original, List.of(finalContour), true, new Scalar(255, 255, 255), 2);

        // 14. Save output
        String outputPath = "final_boundary.png";
        Imgcodecs.imwrite(outputPath, original);
        System.out.println("Boundary drawn successfully. Output: " + outputPath);

        return original;
    }

    private static MatOfPoint getLongestContourByPerimeter(List<MatOfPoint> contours) {
        double maxPerimeter = 0;
        MatOfPoint longest = null;
        for (MatOfPoint contour : contours) {
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            if (perimeter > maxPerimeter) {
                maxPerimeter = perimeter;
                longest = contour;
            }
        }
        return longest;
    }

    private static boolean isContourClosed(List<Point> points, double maxDistance) {
        if (points.size() < 3) return false;
        return distance(points.get(0), points.get(points.size() - 1)) < maxDistance;
    }

    private static double distance(Point p1, Point p2) {
        return Math.hypot(p1.x - p2.x, p1.y - p2.y);
    }
}
