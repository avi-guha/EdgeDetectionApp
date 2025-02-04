package Detection;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class EdgeDetectionApp extends JFrame {
    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private JTextField filePathField;
    private JButton selectButton;
    private JButton saveButton;
    private JButton runButton;
    private JLabel imageLabel;
    private String selectedImagePath;
    private Mat processedImage;

    public EdgeDetectionApp() {
        super("Edge Detection Application");
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        // Top panel for file selection
        JPanel topPanel = new JPanel(new BorderLayout());
        filePathField = new JTextField();
        filePathField.setEditable(false);
        selectButton = new JButton("Select Image");

        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.add(new JLabel("Input Image: "), BorderLayout.WEST);
        filePanel.add(filePathField, BorderLayout.CENTER);
        filePanel.add(selectButton, BorderLayout.EAST);

        // Run and save buttons
        runButton = new JButton("Process Image");
        saveButton = new JButton("Save Output");
        saveButton.setEnabled(false); // Initially disabled until the image is processed

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(runButton);
        buttonPanel.add(saveButton);

        topPanel.add(filePanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Label to display processed image
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);

        // Button listeners
        selectButton.addActionListener(e -> selectImage());
        runButton.addActionListener(e -> processImage());
        saveButton.addActionListener(e -> saveProcessedImage());
    }

    private void selectImage() {
        FileDialog fileDialog = new FileDialog(this, "Select an Image", FileDialog.LOAD);
        fileDialog.setVisible(true);
        if (fileDialog.getFile() != null) {
            selectedImagePath = fileDialog.getDirectory() + fileDialog.getFile();
            filePathField.setText(selectedImagePath);
        }
    }

    private void processImage() {
        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an image first.");
            return;
        }

        // Process the image using the EdgeDetection class
        processedImage = EdgeDetection.processImage(selectedImagePath);
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Processing failed.");
            return;
        }

        // Enable the save button after processing
        saveButton.setEnabled(true);

        // Display the processed image
        ImageIcon icon = new ImageIcon(matToImage(processedImage));
        imageLabel.setIcon(icon);
        this.repaint();

        JOptionPane.showMessageDialog(this, "Image processed successfully!");
    }

    private void saveProcessedImage() {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "No processed image to save.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Processed Image");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Image", "png"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String savePath = fileToSave.getAbsolutePath();

            // Ensure the file has the ".png" extension
            if (!savePath.toLowerCase().endsWith(".png")) {
                savePath += ".png";
            }

            boolean success = Imgcodecs.imwrite(savePath, processedImage);
            if (success) {
                JOptionPane.showMessageDialog(this, "Image saved successfully at:\n" + savePath);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save the processed image.");
            }
        }
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
