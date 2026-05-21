#!/usr/bin/env bash
# LiteDS i18n coverage / leakage check.
# Run from the repo root (melonDS-android/).
#   bash tools/i18n_check.sh
set -u
RES="app/src/main/res"
DEFAULT="$RES/values/strings.xml"
FAIL=0

# LiteDS-specific keys that must exist in ko and ja (41 keys; URL keys excluded).
LITEDS_KEYS=(hide_auxiliary_buttons hide_auxiliary_buttons_summary about about_summary \
  about_tagline about_support_title about_support_desc about_kofi about_opensource_title \
  about_opensource_desc about_github_label about_review_label about_version about_back \
  about_melonds_credit about_melonds_label onboarding_skip onboarding_step1_desc \
  onboarding_step1_button onboarding_step2_title onboarding_step2_desc onboarding_step2_button \
  onboarding_step2_skip onboarding_step3_title onboarding_step3_desc onboarding_step3_button \
  settings_show_advanced settings_show_advanced_summary pause_menu_resume pause_menu_save \
  pause_menu_load pause_menu_reset pause_menu_exit pause_menu_cheats pause_menu_save_slots \
  pause_menu_slot_label pause_menu_empty_slot pause_menu_reset_confirm_title \
  pause_menu_reset_confirm_msg pause_menu_exit_confirm_title pause_menu_exit_confirm_msg)

# Byte-pattern for Hangul syllables in UTF-8 (lead bytes EA-ED). LC_ALL=C avoids
# grep -P unicode-range incompatibility across environments.
HANGUL='[\xEA-\xED][\x80-\xBF][\x80-\xBF]'

# 1) default (values/) must contain NO Hangul.
if LC_ALL=C grep -nP "$HANGUL" "$DEFAULT" >/dev/null 2>&1; then
  echo "FAIL: Hangul found in default values/strings.xml"; FAIL=1
fi

# 2) ko & ja must include every LiteDS key.
for loc in values-ko values-b+ja; do
  f="$RES/$loc/strings.xml"
  if [ ! -f "$f" ]; then echo "FAIL: missing $f"; FAIL=1; continue; fi
  for k in "${LITEDS_KEYS[@]}"; do
    grep -q "name=\"$k\"" "$f" || { echo "FAIL: $loc missing key $k"; FAIL=1; }
  done
done

# 3) No Hangul in any non-ko locale (including default).
for f in "$RES"/values/strings.xml "$RES"/values-b+*/strings.xml; do
  case "$f" in
    *values-ko*) continue;;
  esac
  [ -f "$f" ] || continue
  if LC_ALL=C grep -nP "$HANGUL" "$f" >/dev/null 2>&1; then
    echo "FAIL: Hangul leaked into $f"; FAIL=1
  fi
done

if [ "$FAIL" -eq 0 ]; then echo "i18n check: PASS"; else echo "i18n check: FAIL"; fi
exit $FAIL
