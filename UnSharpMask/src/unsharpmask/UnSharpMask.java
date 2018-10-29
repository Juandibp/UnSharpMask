/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unsharpmask;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/**
 *
 * @author juand
 */
public class UnSharpMask {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String Directory = "../Paper AA/";
        String imageFilename = "bacteria.jpg";

        File originalImgFile = new File(Directory+imageFilename);
        File blurredImgFile = new File(Directory+"blurred_" + imageFilename);
        File usmImgFile = new File(Directory+"sharpened_" + imageFilename);

        BufferedImage loadedImage = null;
        long startTime = System.currentTimeMillis();
        System.out.printf("At %tT.%<tL Loading image '%s' from disk...\n", startTime, imageFilename);
        try {
            loadedImage = ImageIO.read(originalImgFile);
        } catch (Exception e) {
            System.out.println("Eror opening image" + e.toString());
        }
        int imgWidth = loadedImage.getWidth();
        int imgHeight = loadedImage.getHeight();

        System.out.printf("Loaded image '%s' of size (%d x %d) pixels to an BuffredImage object..\n",
                imageFilename, imgWidth, imgHeight);

        //the following line defines the small box for filter kernel
        int boxWidth = 20, boxHeight = 20;
        //the parameters, (left, top, seWidth, selHeight) defines a selection of original image
        int left = 0, top = 0; //left and top should be more than half of box width and height
        //selection width and height is chosen to center the selection
        //can also be used any given selection
        int right = (imgWidth), bottom = (imgHeight);

        //USM filter parameters
        float usmAmount = 0.6f;
        int usmThreshold = 5;

        int[][] origPixels = loadFromImgBuffer(loadedImage);
        System.out.printf("Loaded BuffredImage RGB data to a 2D array...\n");
        int[][] blurredPixels = new int[imgWidth][imgHeight];

       // System.out.printf("Now applying boxcar blur filter. Please wait...\n");
        //applying the boxcar filter with given parameters and save the result in 
        //another 2D array called blurredImage
        try {
            //both the loadedImage and blurredPixels will hold the result of the boxcar function and 
            /*boxCar(loadedImage, origPixels, blurredPixels, left, top,
                    right, bottom, boxWidth, boxHeight);
            System.out.printf("Filtering finished, saving the blurred image.\n");
            //save the loadedImage imagebuffer to a new file
            try {
                ImageIO.write(loadedImage, "jpg", blurredImgFile);
                System.out.printf("Saved the blurred image '%s'\n", blurredImgFile.getName());
            } catch (Exception e) {
                System.out.println("Eror saving blurred image" + e.toString());
            }
*/
            System.out.printf("Now applying unsharp mask filter. Please wait...\n");
            //sharpenes the image using USM and it back to loadedImage ImageBuffer object 
            unsharpMask(loadedImage, origPixels, blurredPixels, left, top,
                    right, bottom, usmAmount, usmThreshold);

            //save the imagebuffer to a new file
            try {
                ImageIO.write(loadedImage, "jpg", usmImgFile);
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.printf("Elapsed %5d mS, saved the sharpened image '%s'\n", elapsed, usmImgFile.getName());
            } catch (Exception e) {
                System.out.println("Eror saving sharpened image" + e.toString());
            }

        } catch (Exception e) {
            System.out.printf("Image selection is probably too large:(%d, %d, %d, %d)\n%s\n",
                    left, top, right, bottom, e.toString());
        }
    }

    //This is the boxcar filter function accepting a 2D array of RGB pixels,
    //A selection for the filter area and a filter kernel size.
    //it also fills an imagebuffer object with blurred image data, which can be saved to disk
    private static void boxCar(BufferedImage loadedImage, int[][] origPixels,
            int[][] blurredPixels, int left, int top, int right, int bottom,
            int filtX, int filtY) throws ArrayIndexOutOfBoundsException {

        //a boundaries of a small 2d array containing adjacent box of pixels for a given
        //pixel is sent to blurPixels function. This function processes for R,G,B and
        //alpha bytes in each pixel in the box seperately and reconstucts the averaged pixel

        for (int j = top; j < bottom; j++) {
            for (int i = left; i < right; i++) {

                //blur pixels using averaging a box of pixels surrounding the given
                //pixel (i,j) and saving the result in both blurredPixels and
                //loadedImage ImageBuffer object
                blurredPixels[i][j] = blurPixels(origPixels, (i - filtX / 2), (j - filtY / 2),
                        (i + filtX / 2), (j + filtY / 2));
                loadedImage.setRGB(i, j, blurredPixels[i][j]);
            }
        }
    }

    //This function returns a RGB value taking the mean of
    //RGB values of each pixel in the filter kernel box
    private static int blurPixels(int[][] origPixels, int left, int top, int right, int bottom) {
        //transperency is not considered
        int alpha = 0xff000000, red = 0, green = 0, blue = 0;
        int boxSize = (right - left) * (bottom - top);

        //the following nested for loops takes the sum of RGB components of each
        //pixels in the box.
        for (int q = top; q < bottom; q++) {
            for (int p = left; p < right; p++) {
                int pixel = origPixels[p][q];
                red += ((pixel >> 16) & 0xff);
                green += ((pixel >> 8) & 0xff);
                blue += ((pixel) & 0xff);
            }
        }
        //average is computed using integer arithmetic. If the box size is too large
        //this routine will fail. max box size = (INT_Max/256) = 8,388,608
        red /= boxSize;
        green /= boxSize;
        blue /= boxSize;
        //returns the reconstructed pixel back
        return (alpha | (red << 16) | (green << 8) | blue);
    }

    //this function calculates the unsharpmask by substracting originalpixels
    //by blurred pixels. Then it sharpenes the original image by adding a 
    //weighted amount of the unsharpmask back to the original depending on
    //the given threshold value
    private static void unsharpMask(BufferedImage usmImage,
            int[][] origPixels, int[][] blurredPixels, int left,
            int top, int right, int bottom, float amount, int threshold) {

        int orgRed = 0, orgGreen = 0, orgBlue = 0;
        int blurredRed = 0, blurredGreen = 0, blurredBlue = 0;
        int usmPixel = 0;
        int alpha = 0xFF000000; //transperency is not considered and always zero

        for (int j = top; j < bottom; j++) {
            for (int i = left; i < right; i++) {
                int origPixel = origPixels[i][j], blurredPixel = blurredPixels[i][j];

                //seperate RGB values of original and blurred pixels into seperate R,G and B values 
                orgRed = ((origPixel >> 16) & 0xff);
                orgGreen = ((origPixel >> 8) & 0xff);
                orgBlue = (origPixel & 0xff);
                blurredRed = ((blurredPixel >> 16) & 0xff);
                blurredGreen = ((blurredPixel >> 8) & 0xff);
                blurredBlue = (blurredPixel & 0xff);

                //If the absolute val. of difference between original and blurred
                //values are greater than the given threshold add weighed difference 
                //back to the original pixel. If the result is outside (0-255),
                //change it back to the corresponding margin 0 or 255   
                if (Math.abs(orgRed - blurredRed) >= threshold) {
                    orgRed = (int) (amount * (orgRed - blurredRed) + orgRed);
                    orgRed = orgRed > 255 ? 255 : orgRed < 0 ? 0 : orgRed;
                }

                if (Math.abs(orgGreen - blurredGreen) >= threshold) {
                    orgGreen = (int) (amount * (orgGreen - blurredGreen) + orgGreen);
                    orgGreen = orgGreen > 255 ? 255 : orgGreen < 0 ? 0 : orgGreen;
                }

                if (Math.abs(orgBlue - blurredBlue) >= threshold) {
                    orgBlue = (int) (amount * (orgBlue - blurredBlue) + orgBlue);
                    orgBlue = orgBlue > 255 ? 255 : orgBlue < 0 ? 0 : orgBlue;
                }

                usmPixel = (alpha | (orgRed << 16) | (orgGreen << 8) | orgBlue);
                usmImage.setRGB(i, j, usmPixel);
            }
        }
    }

    //function to load ARGB values of each pixel in to a 2D array
    static int[][] loadFromImgBuffer(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int[][] pixels = new int[width][height];
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                pixels[i][j] = image.getRGB(i, j);
            }
        }
        return pixels;
    }
    
}
