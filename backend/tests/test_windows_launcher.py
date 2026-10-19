import os
import re
import subprocess
import unittest
from pathlib import Path


@unittest.skipUnless(os.name == 'nt', 'Windows launcher regression')
class WindowsLauncherTests(unittest.TestCase):
    def test_check_only_selects_native_windows_python(self):
        root = Path(__file__).resolve().parents[2]
        result = subprocess.run(
            ['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass',
             '-File', str(root / 'start-campus-news.ps1'), '-CheckOnly'],
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, encoding='utf-8', errors='replace',
            timeout=60)
        paths = re.findall(r'[A-Za-z]:[^\r\n]*python.exe', result.stdout, re.I)
        self.assertTrue(paths, result.stdout)
        selected = paths[-1]
        platform = subprocess.check_output([selected, '-c', 'import sysconfig; print(sysconfig.get_platform())'], text=True).strip()
        self.assertTrue(platform.startswith('win'), result.stdout)
        self.assertEqual(0, result.returncode, result.stdout)
