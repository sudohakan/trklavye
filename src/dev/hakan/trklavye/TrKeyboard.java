package dev.hakan.trklavye;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import android.view.inputmethod.InputConnection;

/**
 * Hardware-keyboard-only input method: the TV firmware has no physical keyboard
 * layout picker, so a US-mapped Bluetooth keyboard is translated to Turkish Q here,
 * by Linux scan code (independent of the system key layout).
 */
public class TrKeyboard extends InputMethodService {

    // Linux evdev scan codes.
    private static final int SC_1 = 2, SC_2 = 3, SC_3 = 4, SC_4 = 5, SC_5 = 6,
            SC_6 = 7, SC_7 = 8, SC_8 = 9, SC_9 = 10, SC_0 = 11,
            SC_MINUS = 12, SC_EQUAL = 13, SC_Q = 16, SC_E = 18, SC_I = 23,
            SC_LEFTBRACE = 26, SC_RIGHTBRACE = 27, SC_SEMICOLON = 39,
            SC_APOSTROPHE = 40, SC_GRAVE = 41, SC_BACKSLASH = 43,
            SC_COMMA = 51, SC_DOT = 52, SC_SLASH = 53, SC_102ND = 86;

    /** {plain, shifted, altGr}; null altGr = not mapped. Letters get caps lock. */
    private static final class Key {
        final String plain, shifted, altGr;
        final boolean letter;

        Key(String plain, String shifted, String altGr, boolean letter) {
            this.plain = plain;
            this.shifted = shifted;
            this.altGr = altGr;
            this.letter = letter;
        }
    }

    private static final SparseArray<Key> MAP = new SparseArray<>();

    static {
        letter(SC_SEMICOLON, "ş", "Ş", "´");
        letter(SC_APOSTROPHE, "i", "İ", null);
        letter(SC_LEFTBRACE, "ğ", "Ğ", "¨");
        letter(SC_RIGHTBRACE, "ü", "Ü", "~");
        letter(SC_COMMA, "ö", "Ö", null);
        letter(SC_DOT, "ç", "Ç", null);
        letter(SC_I, "ı", "I", null);
        symbol(SC_SLASH, ".", ":", null);
        symbol(SC_GRAVE, "\"", "é", "<");
        symbol(SC_MINUS, "*", "?", "\\");
        symbol(SC_EQUAL, "-", "_", "|");
        symbol(SC_BACKSLASH, ",", ";", "`");
        symbol(SC_102ND, "<", ">", "|");
        symbol(SC_1, "1", "!", ">");
        symbol(SC_2, "2", "'", "£");
        symbol(SC_3, "3", "^", "#");
        symbol(SC_4, "4", "+", "$");
        symbol(SC_5, "5", "%", "½");
        symbol(SC_6, "6", "&", null);
        symbol(SC_7, "7", "/", "{");
        symbol(SC_8, "8", "(", "[");
        symbol(SC_9, "9", ")", "]");
        symbol(SC_0, "0", "=", "}");
        altGrOnly(SC_Q, "@");
        altGrOnly(SC_E, "€");
    }

    private static void letter(int sc, String plain, String shifted, String altGr) {
        MAP.put(sc, new Key(plain, shifted, altGr, true));
    }

    private static void symbol(int sc, String plain, String shifted, String altGr) {
        MAP.put(sc, new Key(plain, shifted, altGr, false));
    }

    private static void altGrOnly(int sc, String altGr) {
        MAP.put(sc, new Key(null, null, altGr, false));
    }

    private final SparseArray<Boolean> consumedDown = new SparseArray<>();

    private KeyGridView grid;

    @Override
    public View onCreateInputView() {
        grid = new KeyGridView(this);
        grid.setListener(this::onScreenKey);
        return grid;
    }

    private void onScreenKey(String label) {
        InputConnection ic = getCurrentInputConnection();
        switch (label) {
            case KeyGridView.SHIFT:
                grid.setShifted(!grid.isShifted());
                return;
            case KeyGridView.SYMBOLS:
            case KeyGridView.LETTERS:
                grid.toggleSymbols();
                return;
            case KeyGridView.SWITCH:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchToNextInputMethod(false);
                }
                return;
            case KeyGridView.BACKSPACE:
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                return;
            case KeyGridView.ENTER:
                if (!sendDefaultEditorAction(true)) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
                }
                return;
            default:
                break;
        }
        if (ic == null) {
            return;
        }
        String text = label.equals(KeyGridView.SPACE) ? " " : grid.display(label);
        ic.commitText(text, 1);
        if (grid.isShifted()) {
            grid.setShifted(false);
        }
    }

    /** Diagnostics: shows key/device info as a toast when enabled. */
    private static final boolean DEBUG = false;

    private void debug(String msg) {
        if (DEBUG) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /** Show the on-screen grid unless a real (non-virtual, alphabetic) keyboard is attached. */
    @Override
    public boolean onEvaluateInputViewShown() {
        return !hasAlphabeticKeyboard();
    }

    private static boolean hasAlphabeticKeyboard() {
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d != null && isAlphabetic(d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAlphabetic(InputDevice d) {
        boolean external = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || d.isExternal();
        return !d.isVirtual() && external
                && d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (DEBUG) {
            StringBuilder sb = new StringBuilder("aygıtlar:");
            for (int id : InputDevice.getDeviceIds()) {
                InputDevice d = InputDevice.getDevice(id);
                if (d != null && d.getKeyboardType() != InputDevice.KEYBOARD_TYPE_NONE) {
                    sb.append(" [").append(d.getName()).append(" t=").append(d.getKeyboardType())
                      .append(d.isVirtual() ? " sanal" : "").append("]");
                }
            }
            debug(sb.toString());
        }
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        consumedDown.clear();
        remoteKeyDown = false;
    }

    /**
     * Remote D-pad keys drive the on-screen grid while it is visible; returns true when consumed.
     * Arrow/Enter keys of a physical keyboard keep their normal meaning (cursor, newline).
     */
    private boolean handleRemoteKey(int keyCode, KeyEvent event) {
        if (grid == null || !isInputViewShown() || isPhysicalKeyboard(event) || !isRemoteKey(keyCode)) {
            return false;
        }
        boolean repeat = event.getRepeatCount() > 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: grid.move(-1, 0); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: grid.move(1, 0); break;
            case KeyEvent.KEYCODE_DPAD_LEFT: grid.move(0, -1); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: grid.move(0, 1); break;
            case KeyEvent.KEYCODE_BACK:
                if (!repeat) {
                    requestHideSelf(0);
                }
                break;
            default:
                // Holding select repeats only backspace, like a hardware key; other keys fire once.
                if (!repeat || grid.isBackspaceFocused()) {
                    grid.press();
                }
                break;
        }
        return true;
    }

    private static boolean isRemoteKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BACK:
                return true;
            default:
                return false;
        }
    }

    private boolean remoteKeyDown;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DEBUG) {
            InputDevice d = event.getDevice();
            debug("kod=" + keyCode + " sc=" + event.getScanCode() + " aygıt="
                    + (d == null ? "yok" : d.getName() + " t=" + d.getKeyboardType()
                    + (d.isVirtual() ? " sanal" : "")));
        }
        if (handleRemoteKey(keyCode, event)) {
            remoteKeyDown = true;
            return true;
        }
        String text = translate(event);
        InputConnection ic = getCurrentInputConnection();
        if (text == null || ic == null) {
            return super.onKeyDown(keyCode, event);
        }
        ic.commitText(text, 1);
        consumedDown.put(event.getScanCode(), Boolean.TRUE);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (remoteKeyDown && isRemoteKey(keyCode) && !isPhysicalKeyboard(event)) {
            remoteKeyDown = false;
            return true;
        }
        if (consumedDown.get(event.getScanCode()) != null) {
            consumedDown.remove(event.getScanCode());
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private static String translate(KeyEvent event) {
        if (!isPhysicalKeyboard(event) || event.isCtrlPressed() || event.isMetaPressed()) {
            return null;
        }
        Key key = MAP.get(event.getScanCode());
        if (key == null) {
            return null;
        }
        boolean altGr = (event.getMetaState() & KeyEvent.META_ALT_RIGHT_ON) != 0;
        if (altGr) {
            return key.altGr;
        }
        if (event.isAltPressed() || key.plain == null) {
            return null;
        }
        boolean upper = event.isShiftPressed() ^ (key.letter && event.isCapsLockOn());
        return upper ? key.shifted : key.plain;
    }

    private static boolean isPhysicalKeyboard(KeyEvent event) {
        InputDevice device = event.getDevice();
        return device != null && isAlphabetic(device);
    }
}
