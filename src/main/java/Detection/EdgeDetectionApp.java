package Detection;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EdgeDetectionApp extends JFrame {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private JButton selectButton;
    private JButton processButton;
    private JButton processWhiteButton;  // New button for combined white background processing
    private JButton clearButton;
    private List<Mat> processedImages;

    public EdgeDetectionApp() {
        super("Edge Detection Application");
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new BorderLayout());

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        JScrollPane fileListScrollPane = new JScrollPane(fileList);

        selectButton = new JButton("Add Images");
        processButton = new JButton("Process Images");
        processWhiteButton = new JButton("Process White Background Combined");
        clearButton = new JButton("Clear List");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(selectButton);
        buttonPanel.add(processButton);
        buttonPanel.add(processWhiteButton);
        buttonPanel.add(clearButton);

        topPanel.add(new JLabel("Selected Images:"), BorderLayout.NORTH);
        topPanel.add(fileListScrollPane, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.CENTER);

        // Button listeners
        selectButton.addActionListener(e -> selectImages());
        processButton.addActionListener(e -> processImages());
        processWhiteButton.addActionListener(e -> processWhiteBackgroundCombined());
        clearButton.addActionListener(e -> clearFileList());
    }

    private void selectImages() {
        FileDialog fileDialog = new FileDialog(this, "Select Images", FileDialog.LOAD);
        fileDialog.setMultipleMode(true);
        fileDialog.setVisible(true);

        File[] files = fileDialog.getFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!fileListModel.contains(file.getAbsolutePath())) {
                    fileListModel.addElement(file.getAbsolutePath());
                }
            }
        }
    }

    private void processImages() {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No images selected for processing.");
            return;
        }

        processedImages = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) {
            String imagePath = fileListModel.get(i);

            // Process the image using the original EdgeDetection function
            Mat processedImage = EdgeDetection.processImage(imagePath);
            if (processedImage != null) {
                processedImages.add(processedImage);
                showProcessedImage(processedImage, new File(imagePath).getName());
            }
        }

        JOptionPane.showMessageDialog(this, "All selected images have been processed.");
    }

    // New method: Process white background by overlaying all boundaries on the same white image.
    private void processWhiteBackgroundCombined() {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No images selected for processing.");
            return;
        }
        // Use the size of the first image as the base for the white background.
        String firstImagePath = fileListModel.get(0);
        Mat firstImage = Imgcodecs.imread(firstImagePath);
        if (firstImage.empty()) {
            JOptionPane.showMessageDialog(this, "Error loading the first image.");
            return;
        }
        // Create a white background of the same size and type.
        Mat whiteBackground = new Mat(firstImage.size(), firstImage.type(), new Scalar(255, 255, 255));

        // Iterate over all selected images and overlay their detected boundaries.
        for (int i = 0; i < fileListModel.size(); i++) {
            String imagePath = fileListModel.get(i);
            // Use the new helper method to get the filtered boundary for this image.
            MatOfPoint boundary = EdgeDetection.getFilteredBoundary(imagePath);
            if (boundary != null && !boundary.empty()) {
                // Draw the boundary in black onto the white background.
                Imgproc.polylines(whiteBackground, List.of(boundary), true, new Scalar(0, 0, 0), 2);
            }
        }
        // Display and save the combined white background image.
        showProcessedImage(whiteBackground, "Combined White Background");
        String outputPath = "final_boundary_white_combined.png";
        Imgcodecs.imwrite(outputPath, whiteBackground);
        JOptionPane.showMessageDialog(this, "Combined white background image processed. Output: " + outputPath);
    }

    private void showProcessedImage(Mat image, String title) {
        JFrame imageFrame = new JFrame("Processed: " + title);
        imageFrame.setSize(800, 600);
        imageFrame.setLocationRelativeTo(null);
        imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(new ImageIcon(matToImage(image)));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);

        JButton saveButton = new JButton("Save Image");
        saveButton.addActionListener((ActionEvent e) -> saveImage(image));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        imageFrame.add(panel);
        imageFrame.setVisible(true);
    }

    private void saveImage(Mat image) {
        FileDialog fileDialog = new FileDialog(this, "Save Processed Image", FileDialog.SAVE);
        fileDialog.setFile("processed_image.png"); // Default filename
        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        String filename = fileDialog.getFile();

        if (directory != null && filename != null) {
            // Ensure filename has a .png extension
            if (!filename.toLowerCase().endsWith(".png")) {
                filename += ".png";
            }

            String outputPath = directory + filename;
            boolean success = Imgcodecs.imwrite(outputPath, image);

            if (success) {
                JOptionPane.showMessageDialog(this, "Image saved successfully: " + outputPath);
            } else {
                JOptionPane.showMessageDialog(this, "Error saving the image!", "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFileList() {
        fileListModel.clear();
        JOptionPane.showMessageDialog(this, "File list cleared.");
    }

    // Utility: Convert Mat to Image
    private Image matToImage(Mat mat) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", mat, mob);
        byte[] byteArray = mob.toArray();
        return Toolkit.getDefaultToolkit().createImage(byteArray);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EdgeDetectionApp::new);
    }
}
