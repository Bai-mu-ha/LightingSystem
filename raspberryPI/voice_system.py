import pyaudio
import wave
import os
import sys
import signal
import requests
from array import array
from aip import AipSpeech

# 音频配置
FORMAT = pyaudio.paInt16
CHANNELS = 1
#RATE = 16000
RATE = 48000
CHUNK_SIZE = 512

# 百度语音配置
BAIDU_APP_ID = '118743874'
BAIDU_API_KEY = 'wYMji7GlRvRLZ37vFahmPAKN'
BAIDU_SECRET_KEY = 'BPIjqWzR4SJ3csx0ED4ton4HqXdoDWq8'
baidu_client = AipSpeech(BAIDU_APP_ID, BAIDU_API_KEY, BAIDU_SECRET_KEY)

# DeepSeek配置
DEEPSEEK_API = 'https://api.deepseek.com/v1/chat/completions'
DEEPSEEK_KEY = 'sk-c4ef053191394129945c4e265cecdd60'

class VoiceSystem:
    def __init__(self):
        os.environ['PA_ALSA_PLUGHW'] = '1'  # 强制ALSA重采样
        self.pa = pyaudio.PyAudio()
        self.running = False
        signal.signal(signal.SIGINT, self._handle_interrupt)
        
        # 自动检测设备参数
        self._detect_audio_devices()
        self._print_system_info()
        
        self.conversation_history = []

    def _detect_audio_devices(self):
        """自动检测输入输出设备"""
        self.input_device = None
        self.output_device = None
        
        for i in range(self.pa.get_device_count()):
            dev = self.pa.get_device_info_by_index(i)
            # 优先选择USB音频设备
            if dev['maxInputChannels'] > 0 and ('USB' in dev['name'] or 'Audio' in dev['name']):
                self.input_device = dev
            if dev['maxOutputChannels'] > 0 and ('USB' in dev['name'] or 'Audio' in dev['name']):
                self.output_device = dev


        # 回退到默认设备
        if not self.input_device:
            self.input_device = self.pa.get_default_input_device_info()
        if not self.output_device:
            self.output_device = self.pa.get_default_output_device_info()

        # 确保属性存在
        self.input_device_index = self.input_device['index']
        self.output_device_index = self.output_device['index']


    def _print_system_info(self):
        """打印系统音频信息"""
        print("\n===== 音频设备信息 =====")
        print(f"输入设备: {self.input_device['name']} (索引:{self.input_device['index']})")
        print(f"输出设备: {self.output_device['name']} (索引:{self.output_device['index']})")
        print(f"采样率: {RATE}Hz, 位深: {FORMAT}, 声道数: {CHANNELS}")
        print(f"音频块大小: {CHUNK_SIZE} frames")
        print("="*30 + "\n")

    def _handle_interrupt(self, sig, frame):
        self.running = False

    def run(self):
        self.running = True
        try:
            while self.running:
                print("\n等待说话（10秒自动结束）...")
                self.record('input.wav')
                
                text = self.speech_to_text('input.wav')
                if not text:
                    continue
                    
                print(f"\n[用户] {text}")
                
                if "退出" in text:
                    self.say("再见")
                    break
                    
                reply = self.get_ai_reply(text)
                print(f"[AI] {reply}")
                self.say(reply)
                
        finally:
            self.pa.terminate()

    def record(self, filename):
        """录制5秒音频"""
        
        """修改录音时长（单位：秒）"""
        RECORD_SECONDS = 10  # 改为需要的秒数，例如3秒
        
        stream = self.pa.open(
            format=FORMAT,
            channels=CHANNELS,
            rate=RATE,
            input=True,
            frames_per_buffer=CHUNK_SIZE,
            input_device_index=self.input_device_index,
            input_host_api_specific_stream_info=None
        )
        
        frames = []
        for _ in range(0, int(RATE / CHUNK_SIZE * RECORD_SECONDS)):
            data = stream.read(CHUNK_SIZE)
            frames.append(data)
            # 实时打印音量
            vol = max(abs(s) for s in array('h', data))
            # print(f"\r音量: {vol:4d}", end='')
            
        stream.stop_stream()
        stream.close()
        
        with wave.open(filename, 'wb') as wf:
            wf.setnchannels(CHANNELS)
            wf.setsampwidth(self.pa.get_sample_size(FORMAT))
            wf.setframerate(RATE)
            wf.writeframes(b''.join(frames))

    def say(self, text, filename='output.wav'):
        """语音合成"""
        audio = baidu_client.synthesis(text, 'zh', 1, {
            'vol': 5, 'per': 4, 'aue': 6
        })
        
        if isinstance(audio, bytes):
            with open(filename, 'wb') as f:
                f.write(audio)
                
            self.play(filename)

    def play(self, filename):
        """播放音频"""
        wf = wave.open(filename, 'rb')
        stream = self.pa.open(
            format=self.pa.get_format_from_width(wf.getsampwidth()),
            channels=wf.getnchannels(),
            rate=wf.getframerate(),
            output=True,
            output_device_index=self.output_device_index
        )
        
        data = wf.readframes(CHUNK_SIZE)
        while data:
            stream.write(data)
            data = wf.readframes(CHUNK_SIZE)
            
        stream.stop_stream()
        stream.close()

    def speech_to_text(self, filename):
        """添加错误处理"""
        try:
            with open(filename, 'rb') as f:
                result = baidu_client.asr(f.read(), 'wav', 16000, {
                    'dev_pid': 1536  # 普通话输入法模型
                })
                # print("API响应详情:", result)
                if result['err_no'] == 3301:  # 配额不足
                    return "语音服务额度已用完"
                    
                text = result.get('result', [''])[0]
                
                return text + '。' if text and not text.endswith(('。', '!', '?')) else text  # 自动补全标点
                
        except Exception as e:
            print(f"识别错误: {e}")
            return None
        
    def get_ai_reply(self, text):
        """获取AI回复"""
        try:
            # 维护上下文（保留最近3轮对话）
            self.conversation_history = getattr(self, 'conversation_history', [])[-10:] # 3对话=6条消息（user+assistant交替）
            self.conversation_history.append({"role": "user", "content": text})
            
            resp = requests.post(
                DEEPSEEK_API,
                headers={'Authorization': f'Bearer {DEEPSEEK_KEY}'},
                json={
                    "model": "deepseek-chat",
                    "messages": [
                        {"role": "system", "content": "你是一个幽默风趣的语音聊天机器人，名字叫小琪。回答时用1-2句话简短回复，保持口语化，可以适当加入表情或玩笑。"},
                        *self.conversation_history
                    ],
                    "max_tokens": 60,  # 限制回复长度
                    "temperature": 0.8, # 提高随机性，让回复更生动
                    "top_p": 0.9 # 可选：增加多样性（范围0-1）
                },
                timeout=100
            )
            reply = resp.json()['choices'][0]['message']['content']
            self.conversation_history.append({"role": "assistant", "content": reply})
            
            return reply.split('\n')[0][:60]  # 取首行并截断
            
        except requests.exceptions.RequestException as e:
            print(f"API请求失败: {type(e).__name__}: {e}")
            return "请再说一遍"

if __name__ == "__main__":
    vs = VoiceSystem()
    vs.run()
