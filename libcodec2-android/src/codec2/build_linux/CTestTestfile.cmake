# CMake generated Testfile for 
# Source directory: /home/sh/Downloads/hackrf/codec2
# Build directory: /home/sh/Downloads/hackrf/codec2/build_linux
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(test_freedv_get_hash "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/thash")
add_test(test_CML_ldpcut "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/octave; SHORT_VERSION_FOR_CTEST=1 octave --no-gui -qf ldpcut.m")
set_tests_properties(test_CML_ldpcut PROPERTIES  PASS_REGULAR_EXPRESSION "Nerr: 0")
add_test(test_codec2_700c_octave_port "sh" "-c" "
               cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
               ./c2sim /home/sh/Downloads/hackrf/codec2/raw/cq_ref.raw --phase0 --postfilter --dump cq_ref --lpc 10 --dump_pitch_e cq_ref_pitche.txt;
               cd /home/sh/Downloads/hackrf/codec2/build_linux/unittest; ./tnewamp1 /home/sh/Downloads/hackrf/codec2/raw/cq_ref.raw;
               cd /home/sh/Downloads/hackrf/codec2/octave;
               DISPLAY=\"\" octave-cli -qf --eval 'tnewamp1(\"/home/sh/Downloads/hackrf/codec2/build_linux/src/cq_ref\", \"/home/sh/Downloads/hackrf/codec2/build_linux/unittest\")'")
set_tests_properties(test_codec2_700c_octave_port PROPERTIES  PASS_REGULAR_EXPRESSION "fails: 0")
add_test(test_FDMDV_modem_octave_port "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tfdmdv && DISPLAY=\"\" octave-cli --no-gui -qf /home/sh/Downloads/hackrf/codec2/octave/tfdmdv.m")
set_tests_properties(test_FDMDV_modem_octave_port PROPERTIES  PASS_REGULAR_EXPRESSION "fails: 0" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/octave")
add_test(test_COHPSK_modem_octave_port "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tcohpsk && DISPLAY=\"\" octave-cli --no-gui -qf /home/sh/Downloads/hackrf/codec2/octave/tcohpsk.m")
set_tests_properties(test_COHPSK_modem_octave_port PROPERTIES  PASS_REGULAR_EXPRESSION "fails: 0" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/octave")
add_test(test_COHPSK_modem_AWGN_BER "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_get_test_bits - 5600 | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_mod - - | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_ch - - -30  | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_demod - - | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_put_test_bits -")
add_test(test_COHPSK_modem_freq_offset "sh" "-c" "set -x; /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_get_test_bits - 5600 | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_mod - - | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_ch - - -40 -f -20 | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_demod -v - - 2>log.txt | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_put_test_bits - ; ! grep 'lost sync' log.txt")
add_test(test_OFDM_qam16 "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tqam16")
add_test(test_OFDM_modem_octave_port "sh" "-c" "PATH_TO_TOFDM=/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tofdm DISPLAY=\"\" octave-cli --no-gui -qf /home/sh/Downloads/hackrf/codec2/octave/tofdm.m")
set_tests_properties(test_OFDM_modem_octave_port PROPERTIES  PASS_REGULAR_EXPRESSION "fails: 0" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/octave")
add_test(test_OFDM_modem_octave_port_Nc_31 "sh" "-c" "NC=31 PATH_TO_TOFDM=/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tofdm DISPLAY=\"\" octave-cli --no-gui -qf /home/sh/Downloads/hackrf/codec2/octave/tofdm.m")
set_tests_properties(test_OFDM_modem_octave_port_Nc_31 PROPERTIES  PASS_REGULAR_EXPRESSION "fails: 0" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/octave")
add_test(test_OFDM_modem_700D "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ofdm_get_test_bits - | 
                            ./ofdm_mod | 
                            ./ofdm_demod  --testframes > /dev/null")
add_test(test_OFDM_modem_700D_ldpc "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ofdm_get_test_bits - --ldpc | 
                            ./ofdm_mod --ldpc | 
                            ./ofdm_demod --ldpc --testframes > /dev/null")
add_test(test_OFDM_modem_2020_ldpc "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./fsk_get_test_bits - 5000 | 
                            ./ofdm_mod --ldpc --mode 2020 -p 312 | 
                            ./ofdm_demod  --ldpc --mode 2020 -p 312 | 
                            ./fsk_put_test_bits - -q")
add_test(test_OFDM_modem_AWGN_BER "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/src/ofdm_mod --in /dev/zero --ldpc --testframes 60 --txbpf | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_ch - - -20 --Fs 8000 -f -50 | /home/sh/Downloads/hackrf/codec2/build_linux/src/ofdm_demod --out /dev/null --testframes --ldpc --verbose 1")
add_test(test_OFDM_modem_fading_BER "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/ofdm_fade.sh")
set_tests_properties(test_OFDM_modem_fading_BER PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/unittest")
add_test(test_OFDM_modem_phase_est_bw "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/unittest;
                            PATH=\$PATH:/home/sh/Downloads/hackrf/codec2/build_linux/src ./ofdm_phase_est_bw.sh")
add_test(test_OFDM_modem_fading_DPSK_BER "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/unittest;
                            PATH=\$PATH:/home/sh/Downloads/hackrf/codec2/build_linux/src ./ofdm_fade_dpsk.sh")
add_test(test_OFDM_modem_time_sync_700D "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/unittest;
                            PATH=\$PATH:/home/sh/Downloads/hackrf/codec2/build_linux/src ./ofdm_time_sync.sh 700D")
add_test(test_OFDM_modem_datac1_octave "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux;
                            ./src/ofdm_mod --mode datac1 --in /dev/zero --testframes 5 --verbose 1 > test.raw;
                            cd /home/sh/Downloads/hackrf/codec2/octave;
                            DISPLAY=\"\" octave-cli -qf --eval 'ofdm_rx(\"/home/sh/Downloads/hackrf/codec2/build_linux/test.raw\",\"datac1\")'")
set_tests_properties(test_OFDM_modem_datac1_octave PROPERTIES  PASS_REGULAR_EXPRESSION "BER..: 0.0000")
add_test(test_OFDM_modem_datac1 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ofdm_mod   --mode datac1 --in  /dev/zero --testframes 10 --verbose 1 |
                            ./ofdm_demod --mode datac1 --out /dev/null --testframes   --verbose 1")
add_test(test_OFDM_modem_datac1_ldpc "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ofdm_mod   --mode datac1 --in  /dev/zero --testframes 10 --ldpc --verbose 1 | 
                            ./ofdm_demod --mode datac1 --out /dev/null  --testframes    --ldpc --verbose 1")
add_test(test_ldpc_enc_dec "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --sd --code HRA_112_112 --testframes 200 | 
                            ./ldpc_noise - - 0.5 | 
                            ./ldpc_dec - /dev/null --code HRA_112_112 --sd --testframes")
add_test(test_ldpc_enc_dec_HRAb_396_504 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --sd --code HRAb_396_504 --testframes 200 | 
                            ./ldpc_noise - - -2.0 | 
                            ./ldpc_dec - /dev/null --code HRAb_396_504 --sd --testframes")
add_test(test_ldpc_enc_dec_H_256_768_22 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --sd --code H_256_768_22 --testframes 200 | 
                            ./ldpc_noise - - 3.0 | 
                            ./ldpc_dec - /dev/null --code H_256_768_22 --sd --testframes")
add_test(test_ldpc_enc_dec_H_256_512_4 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --sd --code H_256_512_4 --testframes 200 | 
                            ./ldpc_noise - - 0.5 | 
                            ./ldpc_dec - /dev/null --code H_256_512_4 --sd --testframes")
add_test(test_ldpc_enc_dec_HRAa_1536_512 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --sd --code HRAa_1536_512 --testframes 200 | 
                            ./ldpc_noise - - -2 | 
                            ./ldpc_dec - /dev/null --code HRAa_1536_512 --sd --testframes")
add_test(test_ldpc_enc_dec_H_128_256_5 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --sd --code H_128_256_5 --testframes 200 | 
                            ./ldpc_noise - - 0.5 | 
                            ./ldpc_dec - /dev/null --code H_128_256_5 --sd --testframes")
add_test(test_freedv_api_1600 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 1600 ../../raw/ve9qrp_10s.raw - | ./freedv_rx 1600 - /dev/null")
set_tests_properties(test_freedv_api_1600 PROPERTIES  PASS_REGULAR_EXPRESSION "frames decoded: 503")
add_test(test_freedv_api_700C "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 700C ../../raw/ve9qrp_10s.raw - | ./freedv_rx 700C - /dev/null")
set_tests_properties(test_freedv_api_700C PROPERTIES  PASS_REGULAR_EXPRESSION "frames decoded: 125")
add_test(test_freedv_api_700D_backwards_compatability "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/src/freedv_rx 700D /home/sh/Downloads/hackrf/codec2/raw/testframes_700d.raw /dev/null --testframes --discard")
add_test(test_freedv_api_700D_burble "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 700D ../../raw/ve9qrp.raw - | 
                            ./cohpsk_ch - - -10 --Fs 8000 | 
                            ./freedv_rx 700D - /dev/null --squelch -2")
set_tests_properties(test_freedv_api_700D_burble PROPERTIES  PASS_REGULAR_EXPRESSION "frames decoded: 746  output speech samples: 0")
add_test(test_freedv_api_700D_AWGN_BER "sh" "-c" "dd bs=2560 count=120 if=/dev/zero | /home/sh/Downloads/hackrf/codec2/build_linux/src/freedv_tx 700D - - --testframes | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_ch - - -20 --Fs 8000 -f -10 | /home/sh/Downloads/hackrf/codec2/build_linux/src/freedv_rx 700D - /dev/null --testframes --discard")
add_test(test_freedv_api_700D_AWGN_BER_USECOMPLEX "sh" "-c" "dd bs=2560 count=120 if=/dev/zero | /home/sh/Downloads/hackrf/codec2/build_linux/src/freedv_tx 700D - - --testframes | /home/sh/Downloads/hackrf/codec2/build_linux/src/cohpsk_ch - - -20 --Fs 8000 -f -10 | /home/sh/Downloads/hackrf/codec2/build_linux/src/freedv_rx 700D - /dev/null --testframes --discard --usecomplex")
add_test(test_freedv_api_2400A "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 2400A ../../raw/ve9qrp_10s.raw - | ./freedv_rx 2400A - /dev/null")
set_tests_properties(test_freedv_api_2400A PROPERTIES  PASS_REGULAR_EXPRESSION "frames decoded: 250")
add_test(test_freedv_api_2400B "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 2400B ../../raw/ve9qrp_10s.raw - | ./freedv_rx 2400B - /dev/null")
set_tests_properties(test_freedv_api_2400B PROPERTIES  PASS_REGULAR_EXPRESSION "frames decoded: 250")
add_test(test_freedv_api_800XA "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 800XA ../../raw/ve9qrp_10s.raw - | ./freedv_rx 800XA - /dev/null")
set_tests_properties(test_freedv_api_800XA PROPERTIES  PASS_REGULAR_EXPRESSION "frames decoded: 125")
add_test(test_freedv_api_rawdata_800XA "sh" "-c" "./tfreedv_800XA_rawdata")
set_tests_properties(test_freedv_api_rawdata_800XA PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/unittest")
add_test(test_freedv_api_rawdata_2400A "sh" "-c" "./tfreedv_2400A_rawdata")
set_tests_properties(test_freedv_api_rawdata_2400A PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/unittest")
add_test(test_freedv_api_rawdata_2400B "sh" "-c" "./tfreedv_2400B_rawdata")
set_tests_properties(test_freedv_api_rawdata_2400B PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/unittest")
add_test(test_fifo "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tfifo")
add_test(test_memory_leak_FreeDV_1600_tx "sh" "-c" " valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./freedv_tx 1600 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw /dev/null")
set_tests_properties(test_memory_leak_FreeDV_1600_tx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_memory_leak_FreeDV_1600_rx "sh" "-c" "./freedv_tx 1600 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw t.raw;                             valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./freedv_rx 1600 t.raw /dev/null")
set_tests_properties(test_memory_leak_FreeDV_1600_rx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_memory_leak_FreeDV_700D_tx "sh" "-c" " valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./freedv_tx 700D /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw /dev/null")
set_tests_properties(test_memory_leak_FreeDV_700D_tx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_memory_leak_FreeDV_700D_rx "sh" "-c" "./freedv_tx 700D /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw t.raw;                             valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./freedv_rx 700D t.raw /dev/null")
set_tests_properties(test_memory_leak_FreeDV_700D_rx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_memory_leak_FreeDV_700C_tx "sh" "-c" " valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./freedv_tx 700C /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw /dev/null")
set_tests_properties(test_memory_leak_FreeDV_700C_tx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_memory_leak_FreeDV_700C_rx "sh" "-c" "cd  /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_tx 700C /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw t.raw;                             valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes ./freedv_rx 700C t.raw /dev/null")
set_tests_properties(test_memory_leak_FreeDV_700C_rx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors")
add_test(test_memory_leak_FreeDV_FSK_LDPC_tx "sh" "-c" "cd  /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            valgrind --leak-check=full --show-leak-kinds=all --track-origins=yes                             ./freedv_data_raw_tx --testframes 10 FSK_LDPC /dev/zero /dev/null")
set_tests_properties(test_memory_leak_FreeDV_FSK_LDPC_tx PROPERTIES  PASS_REGULAR_EXPRESSION "ERROR SUMMARY: 0 errors")
add_test(test_codec2_mode_dot_c2 "sh" "-c" "./c2enc 700C /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw hts1a.c2 && ./c2dec 1600 hts1a.c2 /dev/null")
set_tests_properties(test_codec2_mode_dot_c2 PROPERTIES  PASS_REGULAR_EXPRESSION "mode 8" WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_3200 "sh" "-c" "./c2enc 3200 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 3200 - - | sox -t .s16 -r 8000 - hts1a_3200.wav")
set_tests_properties(test_codec2_mode_3200 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_2400 "sh" "-c" "./c2enc 2400 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 2400 - - | sox -t .s16 -r 8000 - hts1a_2400.wav")
set_tests_properties(test_codec2_mode_2400 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_1400 "sh" "-c" "./c2enc 1400 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 1400 - - | sox -t .s16 -r 8000 - hts1a_1400.wav")
set_tests_properties(test_codec2_mode_1400 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_1300 "sh" "-c" "./c2enc 1300 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 1300 - - | sox -t .s16 -r 8000 - hts1a_1300.wav")
set_tests_properties(test_codec2_mode_1300 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_1200 "sh" "-c" "./c2enc 1200 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 1200 - - | sox -t .s16 -r 8000 - hts1a_1200.wav")
set_tests_properties(test_codec2_mode_1200 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_700C "sh" "-c" "./c2enc 700C /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 700C - - | sox -t .s16 -r 8000 - hts1a_700C.wav")
set_tests_properties(test_codec2_mode_700C PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_450 "sh" "-c" "./c2enc 450 /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 450 - - | sox -t .s16 -r 8000 - hts1a_450.wav")
set_tests_properties(test_codec2_mode_450 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_codec2_mode_450PWB "sh" "-c" "./c2enc 450PWB /home/sh/Downloads/hackrf/codec2/raw/hts1a.raw - | ./c2dec 450PWB - - | sox -t .s16 -r 16000 - hts1a_450PWB.wav")
set_tests_properties(test_codec2_mode_450PWB PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/src")
add_test(test_est_n0 "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/est_n0.sh")
set_tests_properties(test_est_n0 PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/unittest")
add_test(test_vq_mbest "sh" "-c" "./tvq_mbest;                             cat target.f32 | ../misc/vq_mbest -k 2 -q vq1.f32,vq2.f32 --mbest 2 -v > out.f32;                             diff target.f32 out.f32")
set_tests_properties(test_vq_mbest PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/unittest")
add_test(test_700c_eq "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/test_700c_eq.sh")
set_tests_properties(test_700c_eq PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/unittest")
add_test(test_fsk_lib "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/octave; DISPLAY=\"\" octave --no-gui -qf fsk_lib_demo.m")
set_tests_properties(test_fsk_lib PROPERTIES  PASS_REGULAR_EXPRESSION "PASS")
add_test(test_fsk_modem_octave_port "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/octave; 
                            PATH_TO_TFSK=/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tfsk octave --no-gui -qf tfsk.m")
set_tests_properties(test_fsk_modem_octave_port PROPERTIES  PASS_REGULAR_EXPRESSION "PASS")
add_test(test_fsk_modem_mod_demod "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/src/fsk_get_test_bits - 10000 | 
                            /home/sh/Downloads/hackrf/codec2/build_linux/src/fsk_mod 2 8000 100 1200 100 - - | 
                            /home/sh/Downloads/hackrf/codec2/build_linux/src/fsk_demod -l 2 8000 100 - - | 
                            /home/sh/Downloads/hackrf/codec2/build_linux/src/fsk_put_test_bits -p 99 -q -")
add_test(test_fsk_2fsk_ber "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            ./fsk_get_test_bits - 10000 | ./fsk_mod 2 8000 100 1000 100 - - | 
                            ./cohpsk_ch - - -26 --Fs 8000 | 
                            ./fsk_demod 2 8000 100 - - | ./fsk_put_test_bits -b 0.015 -q - ")
add_test(test_fsk_4fsk_ber "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            ./fsk_get_test_bits - 10000 | ./fsk_mod 4 8000 100 1000 100 - - | 
                            ./cohpsk_ch - - -26 --Fs 8000 | 
                            ./fsk_demod 4 8000 100 - - | ./fsk_put_test_bits -b 0.025 - ")
add_test(test_fsk_4fsk_ber_negative_freq "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            ./fsk_get_test_bits - 10000 | ./fsk_mod 4 8000 100 1000 200 - - | 
                            ./cohpsk_ch - - -23 --Fs 8000 --ssbfilt 0 --complexout -f -4000 | 
                            ./fsk_demod -c -p 8 4 8000 100 - - | 
                            ./fsk_put_test_bits -b 0.025 -q - ")
add_test(test_fsk_4fsk_lockdown "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            bits=512; tx_packets=20; rx_packets=18; tx_tone_sep=270; Rs=25; 
                            ./fsk_get_test_bits - \$((\$bits*\$tx_packets)) \$bits | 
                            ./fsk_mod 4 8000 \$Rs 1000 \$tx_tone_sep - - | 
                            ./cohpsk_ch - - -13 --Fs 8000 --ssbfilt 0 -f -3000 --complexout | 
                            ./fsk_demod -c -p 8 --mask \$tx_tone_sep -t1 --nsym 100 4 8000 \$Rs - - 2>stats.txt | 
                            ./fsk_put_test_bits -t 0.25 -b 0.20 -p \$rx_packets -f \$bits -q -")
add_test(test_fsk_lib_4fsk_ldpc "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/octave; DISPLAY=\"\" octave --no-gui -qf fsk_lib_ldpc_demo.m")
set_tests_properties(test_fsk_lib_4fsk_ldpc PROPERTIES  PASS_REGULAR_EXPRESSION "PASS")
add_test(test_fsk_framer "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            ./fsk_get_test_bits - 300 | 
                            ./framer - - 100 51 | 
                            ./deframer - - 100 51 --hard | 
                            ./fsk_put_test_bits -")
set_tests_properties(test_fsk_framer PROPERTIES  PASS_REGULAR_EXPRESSION "PASS")
add_test(test_fsk_framer_ldpc "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            ./ldpc_enc /dev/zero - --code HRA_112_112 --testframes 10 | ./framer - - 224 51 | 
                            ./tollr | ./deframer - - 224 51 | ./ldpc_dec - /dev/null --code HRA_112_112 --testframes")
add_test(test_fsk_llr "sh" "-c" "/home/sh/Downloads/hackrf/codec2/build_linux/unittest/tfsk_llr")
add_test(test_fsk_4fsk_ldpc "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./ldpc_enc /dev/zero - --code HRAb_396_504 --testframes 200 | 
                            ./framer - - 504 5186 |
                            ./fsk_mod 4 8000 100 1000 100 - - | 
                            ./cohpsk_ch - - -25 --Fs 8000  | 
                            ./fsk_demod -s 4 8000 100 - - | 
                            ./deframer - - 504 5186  | 
                            ./ldpc_dec - /dev/null --code HRAb_396_504 --testframes")
add_test(test_fsk_vhf_framer "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src; 
                            ./c2enc 700C ../../raw/ve9qrp_10s.raw - | 
                            ./vhf_frame_c2 B - - | 
                            ./fsk_mod -p 10 4 8000 400 400 400 - - | 
                            ./fsk_demod -p 10 4 8000 400 - - | 
                            ./vhf_deframe_c2 B - /dev/null")
set_tests_properties(test_fsk_vhf_framer PROPERTIES  PASS_REGULAR_EXPRESSION "total_uw_err: 0")
add_test(test_freedv_data_channel "sh" "-c" "./tfreedv_data_channel")
set_tests_properties(test_freedv_data_channel PROPERTIES  WORKING_DIRECTORY "/home/sh/Downloads/hackrf/codec2/build_linux/unittest")
add_test(test_freedv_data_raw_ofdm "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            head -c 140 </dev/urandom > binaryIn.bin;
                            ./freedv_data_raw_tx 700D binaryIn.bin - | 
                            ./freedv_data_raw_rx 700D - - -v | 
                            diff /dev/stdin binaryIn.bin")
add_test(test_freedv_data_raw_fsk_ldpc_100 "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_data_raw_tx --testframes 1 --bursts 10 FSK_LDPC /dev/zero - |
                            ./cohpsk_ch - - -5 --Fs 8000 --ssbfilt 0 |
                            ./freedv_data_raw_rx --testframes -v FSK_LDPC - /dev/null")
set_tests_properties(test_freedv_data_raw_fsk_ldpc_100 PROPERTIES  PASS_REGULAR_EXPRESSION "output_packets: 10")
add_test(test_freedv_data_raw_fsk_ldpc_1k "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_data_raw_tx --Fs 40000 --Rs 1000 --tone1 1000 --shift 1000 --testframes 1 --bursts 10 FSK_LDPC /dev/zero - |
                            ./cohpsk_ch - - -10 --Fs 8000 --ssbfilt 0 |
                            ./freedv_data_raw_rx --testframes -v --Fs 40000 --Rs 1000 FSK_LDPC - /dev/null")
set_tests_properties(test_freedv_data_raw_fsk_ldpc_1k PROPERTIES  PASS_REGULAR_EXPRESSION "output_packets: 10")
add_test(test_freedv_data_raw_fsk_ldpc_10k "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_data_raw_tx --Fs 100000 --Rs 10000 --tone1 10000 --shift 10000 --testframes 100 --bursts 10 FSK_LDPC /dev/zero - |
                            ./cohpsk_ch - - -15 --Fs 8000 --ssbfilt 0 | 
                            ./freedv_data_raw_rx --testframes -v --Fs 100000 --Rs 10000 FSK_LDPC - /dev/null")
set_tests_properties(test_freedv_data_raw_fsk_ldpc_10k PROPERTIES  PASS_REGULAR_EXPRESSION "output_packets: 1000")
add_test(test_freedv_data_raw_fsk_ldpc_2k "sh" "-c" "cd /home/sh/Downloads/hackrf/codec2/build_linux/src;
                            ./freedv_data_raw_tx -a 8192 -m 4 --Fs 40000 --Rs 1000 --tone1 1000 --shift 1000 --testframes 1 --bursts 10 FSK_LDPC /dev/zero - |
                             ./cohpsk_ch - - -22 --Fs 8000 --ssbfilt 0 | 
                             ./freedv_data_raw_rx -m 4 --testframes -v --Fs 40000 --Rs 1000 FSK_LDPC --mask 1000 - /dev/null")
set_tests_properties(test_freedv_data_raw_fsk_ldpc_2k PROPERTIES  PASS_REGULAR_EXPRESSION "output_packets: 10")
subdirs("src")
subdirs("unittest")
subdirs("misc")
