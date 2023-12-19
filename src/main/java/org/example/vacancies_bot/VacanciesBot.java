package org.example.vacancies_bot;

import org.example.vacancies_bot.dto.VacancyDto;
import org.example.vacancies_bot.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VacanciesBot extends TelegramLongPollingBot {
    @Autowired
    VacancyService vacancyService;

    private final Map<Long, String> lastShownVacancyLevel = new HashMap<>();

    public VacanciesBot() {
        super(System.getenv("TG_TOKEN"));
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.getMessage() != null) {
                handleStartCommand(update);
            }

            if (update.getCallbackQuery() != null) {
                String callbackData = update.getCallbackQuery().getData();
                if ("showJuniorVacancies".equals(callbackData)) {
                    showJuniorVacancies(update);
                } else if ("showMiddleVacancies".equals(callbackData)) {
                    showMiddleVacancies(update);
                } else if ("showSeniorVacancies".equals(callbackData)) {
                    showSeniorVacancies(update);
                } else if (callbackData.startsWith("vacancyId=")) {
                    String id = callbackData.split("=")[1];
                    showVacancyDescription(id, update);
                } else if ("backToVacancies".equals(callbackData)) {
                    handleBackToVacanciesCommand(update);
                } else if ("backToStartMenu".equals(callbackData)) {
                    handleBackToStartCommand(update);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Can't send message to a user!", e);
        }
    }

    private void handleBackToStartCommand(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Choose title:");
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setReplyMarkup(getStartMenu());
        execute(sendMessage);

    }

    private void handleBackToVacanciesCommand(Update update) throws TelegramApiException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String level = lastShownVacancyLevel.get(chatId);

        if ("junior".equals(level)) {
            showJuniorVacancies(update);
        } else if ("middle".equals(level)) {
            showMiddleVacancies(update);
        } else if ("senior".equals(level)) {
            showSeniorVacancies(update);
        }
    }

    private void showVacancyDescription(String id, Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        VacancyDto vacancy = vacancyService.get(id);
        String salaryText = vacancy.getSalary().equals("-") ? "Not specified" : vacancy.getSalary();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.enableHtml(true);
        sendMessage.setText(
                "<b>" + vacancy.getTitle() + "</b>" + "\n\n" +
                "<b>Short description:</b> " + vacancy.getShortDescription() + "\n\n" +
                "<b>Description:</b> " + vacancy.getLongDescription() + "\n\n" +
                "<b>Company:</b> " + vacancy.getCompany() + "\n\n" +
                "<b>Salary:</b> " + salaryText + "\n\n" +
                "Navigate the link for more details: " + vacancy.getLink()
        );
        sendMessage.setReplyMarkup(getBackToVacanciesMenu());
        execute(sendMessage);
    }

    private ReplyKeyboard getBackToVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backToVacanciesButton = new InlineKeyboardButton();
        backToVacanciesButton.setText("Back to vacancies");
        backToVacanciesButton.setCallbackData("backToVacancies");
        row.add(backToVacanciesButton);

        InlineKeyboardButton backToStartButton = new InlineKeyboardButton();
        backToStartButton.setText("Back to start menu");
        backToStartButton.setCallbackData("backToStartMenu");
        row.add(backToStartButton);

        return new InlineKeyboardMarkup(List.of(row));
    }

    private void showJuniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setText("Please choose vacancy:");
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getJuniorVacanciesMenu());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "junior");
    }

    private ReplyKeyboard getJuniorVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        List<VacancyDto> vacancies = vacancyService.getJuniorVacancies();

        for (VacancyDto vacancy : vacancies) {
            InlineKeyboardButton vacancyButton = new InlineKeyboardButton();
            vacancyButton.setText(vacancy.getTitle());
            vacancyButton.setCallbackData("vacancyId=" + vacancy.getId());
            row.add(vacancyButton);
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));

        return keyboard;
    }

    private void showMiddleVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setText("Please choose vacancy:");
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getMiddleVacanciesMenu());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "middle");
    }

    private ReplyKeyboard getMiddleVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton maVacancy = new InlineKeyboardButton();
        maVacancy.setText("Middle Java dev at MA");
        maVacancy.setCallbackData("vacancyId=3");
        row.add(maVacancy);

        InlineKeyboardButton googleVacancy = new InlineKeyboardButton();
        googleVacancy.setText("Middle Java dev at Google");
        googleVacancy.setCallbackData("vacancyId=4");
        row.add(googleVacancy);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));

        return keyboard;
    }

    private void showSeniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setText("Please choose vacancy:");
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getSeniorVacanciesMenu());
        execute(sendMessage);

        lastShownVacancyLevel.put(chatId, "senior");
    }

    private ReplyKeyboard getSeniorVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton maVacancy = new InlineKeyboardButton();
        maVacancy.setText("Senior Java dev at MA");
        maVacancy.setCallbackData("vacancyId=5");
        row.add(maVacancy);

        InlineKeyboardButton googleVacancy = new InlineKeyboardButton();
        googleVacancy.setText("Senior Java dev at Google");
        googleVacancy.setCallbackData("vacancyId=6");
        row.add(googleVacancy);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));

        return keyboard;
    }

    private void handleStartCommand(Update update) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Welcome to the vacancies bot! Please, choose your title:");
        sendMessage.setReplyMarkup(getStartMenu());
        execute(sendMessage);
    }

    private ReplyKeyboard getStartMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton junior = new InlineKeyboardButton();
        junior.setText("Junior");
        junior.setCallbackData("showJuniorVacancies");
        row.add(junior);

        InlineKeyboardButton middle = new InlineKeyboardButton();
        middle.setText("Middle");
        middle.setCallbackData("showMiddleVacancies");
        row.add(middle);

        InlineKeyboardButton senior = new InlineKeyboardButton();
        senior.setText("Senior");
        senior.setCallbackData("showSeniorVacancies");
        row.add(senior);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));

        return keyboard;
    }

    @Override
    public String getBotUsername() {
        return "Vacancies bot";
    }
}
