import java.util.*;
import java.lang.*;

// Helper class
public class Helper {
	public static int[] clone1DArr(int[] arr) {
		int arrSize = arr.length;
		int[] newArr = new int[arrSize];

		for (int i=0; i<arrSize; i++) {
			newArr[i] = arr[i];
		}

		return newArr;
	}

	public static ArrayList<Integer> clone1DArrayList(ArrayList<Integer> arr) {
		int arrSize = arr.size();
		ArrayList<Integer> newArr = new ArrayList<Integer>();

		for (int i=0; i<arrSize; i++) {
			newArr.add(arr.get(i));
		}

		return newArr;
	}

	public static int[][] clone2DArr(int[][] arr) {
		int numRow = arr.length;
		int numCol = arr[0].length;
		int[][] newArr = new int[numRow][numCol];

		for (int r=0; r<numRow; r++) {
			newArr[r] = clone1DArr(arr[r]);
		}

		return newArr;
	}

	public static void printArr(int[] arr) {
		for (int i=0; i<arr.length; i++) {
			System.out.print(arr[i] + " ");
		}
		System.out.println();
	}

	public static void print2DArr(int[][] arr) {
		for (int i=0; i<arr.length; i++) {
			printArr(arr[i]);
		}
	}
}