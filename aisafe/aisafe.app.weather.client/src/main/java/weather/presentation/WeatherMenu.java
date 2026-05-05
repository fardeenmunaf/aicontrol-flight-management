package weather.presentation;

import weather.presentation.weatherData.RegisterWeatherDataUI;
import weather.presentation.weatherData.ViewWeatherDataUI;

public class WeatherMenu {

    public void mainLoop() {
        int option;
        do {
            showMenu();
            option = readOption();
            handleOption(option);
        } while (option != 0);
    }

    private void showMenu() {
        System.out.println("Weather Client Menu:");
        System.out.println("1. Register Weather Data");
        System.out.println("2. View Weather Data");
        System.out.println("0. Exit");
    }

    private int readOption() {
        System.out.print("Select an option: ");
        try {
            return Integer.parseInt(System.console().readLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void handleOption(int option) {
        switch (option) {
            case 1:
                new RegisterWeatherDataUI().show();
                break;
            case 2:
                new ViewWeatherDataUI().show();
                break;
            case 0:
                System.out.println("Exiting...");
                break;
            default:
                System.out.println("Invalid option. Please try again.");
        }
    }
}